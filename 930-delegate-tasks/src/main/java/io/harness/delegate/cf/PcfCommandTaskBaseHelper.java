package io.harness.delegate.cf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_INFRA_STATE;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.pcf.model.PcfConstants.BUILDPACKS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.BUILDPACK_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.COMMAND_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DISK_QUOTA_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DOCKER_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.DOMAINS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ENV_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HARNESS__STATUS__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HEALTH_CHECK_HTTP_ENDPOINT_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HEALTH_CHECK_TYPE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HOSTS_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.HOST_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.INSTANCE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.MEMORY_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_HOSTNAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PATH_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PCF_ARTIFACT_DOWNLOAD_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.RANDOM_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.REPOSITORY_DIR_PATH;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_PATH_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.SERVICES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.STACK_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.TIMEOUT_MANIFEST_YML_ELEMENT;

import static software.wings.beans.LogColor.Gray;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.task.pcf.exception.InvalidPcfStateException;
import io.harness.delegate.task.pcf.request.CfCommandDeployRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfCliDelegateResolver;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.jetbrains.annotations.NotNull;

/**
 * Stateles helper class
 */
@Singleton
@Slf4j
@OwnedBy(CDP)
public class PcfCommandTaskBaseHelper {
  public static final String CURRENT_INSTANCE_COUNT = "CURRENT-INSTANCE-COUNT: ";
  public static final String DESIRED_INSTANCE_COUNT = "DESIRED-INSTANCE-COUNT: ";

  public static final String DELIMITER = "__";
  public static final String APPLICATION = "APPLICATION: ";

  @Inject private CfDeploymentManager pcfDeploymentManager;
  @Inject private CfCliDelegateResolver cfCliDelegateResolver;

  public void unmapExistingRouteMaps(ApplicationDetail applicationDetail, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Unmapping routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationDetail.getName());
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(applicationDetail.getUrls()));
    // map
    cfRequestConfig.setApplicationName(applicationDetail.getName());
    pcfDeploymentManager.unmapRouteMapForApplication(
        cfRequestConfig, applicationDetail.getUrls(), executionLogCallback);
  }

  public File createYamlFileLocally(String filePath, String content) throws IOException {
    File file = new File(filePath);
    return writeToManifestFile(content, file);
  }

  /**
   * This is called from Deploy (Resize) phase.
   */
  public void upsizeNewApplication(LogCallback executionLogCallback, CfCommandDeployRequest cfCommandDeployRequest,
      List<CfServiceData> cfServiceDataUpdated, CfRequestConfig cfRequestConfig, ApplicationDetail details,
      List<CfInternalInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("# Upsizing new application:", White, Bold));

    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("APPLICATION-NAME: ")
                                              .append(details.getName())
                                              .append("\n" + CURRENT_INSTANCE_COUNT)
                                              .append(details.getInstances())
                                              .append("\n" + DESIRED_INSTANCE_COUNT)
                                              .append(cfCommandDeployRequest.getUpdateCount())
                                              .toString());

    // Upscale new app
    cfRequestConfig.setApplicationName(cfCommandDeployRequest.getNewReleaseName());
    cfRequestConfig.setDesiredCount(cfCommandDeployRequest.getUpdateCount());

    // perform upsize
    upsizeInstance(
        cfRequestConfig, pcfDeploymentManager, executionLogCallback, cfServiceDataUpdated, pcfInstanceElements);
  }

  private void upsizeInstance(CfRequestConfig cfRequestConfig, CfDeploymentManager pcfDeploymentManager,
      LogCallback executionLogCallback, List<CfServiceData> cfServiceDataUpdated,
      List<CfInternalInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
    // Get application details before upsize
    ApplicationDetail detailsBeforeUpsize = pcfDeploymentManager.getApplicationByName(cfRequestConfig);
    StringBuilder sb = new StringBuilder();

    // create pcfServiceData having all details of this upsize operation
    cfServiceDataUpdated.add(CfServiceData.builder()
                                 .previousCount(detailsBeforeUpsize.getInstances())
                                 .desiredCount(cfRequestConfig.getDesiredCount())
                                 .name(cfRequestConfig.getApplicationName())
                                 .id(detailsBeforeUpsize.getId())
                                 .build());

    // upsize application
    ApplicationDetail detailsAfterUpsize =
        pcfDeploymentManager.upsizeApplicationWithSteadyStateCheck(cfRequestConfig, executionLogCallback);
    executionLogCallback.saveExecutionLog(sb.append("# Application upsized successfully ").toString());

    List<InstanceDetail> newUpsizedInstances = filterNewUpsizedAppInstances(detailsBeforeUpsize, detailsAfterUpsize);
    newUpsizedInstances.forEach(instance
        -> pcfInstanceElements.add(CfInternalInstanceElement.builder()
                                       .uuid(detailsAfterUpsize.getId() + instance.getIndex())
                                       .applicationId(detailsAfterUpsize.getId())
                                       .displayName(detailsAfterUpsize.getName())
                                       .instanceIndex(instance.getIndex())
                                       .isUpsize(true)
                                       .build()));

    // Instance token is ApplicationGuid:InstanceIndex, that can be used to connect to instance from outside world
    List<InstanceDetail> instancesAfterUpsize = new ArrayList<>(detailsAfterUpsize.getInstanceDetails());
    executionLogCallback.saveExecutionLog(
        new StringBuilder().append("\n# Application state details after upsize:  ").toString());
    printApplicationDetail(detailsAfterUpsize, executionLogCallback);
    printInstanceDetails(executionLogCallback, instancesAfterUpsize);
  }

  public void printInstanceDetails(LogCallback executionLogCallback, List<InstanceDetail> instances) {
    StringBuilder builder = new StringBuilder("Instance Details:");
    instances.forEach(instance
        -> builder.append("\nIndex: ")
               .append(instance.getIndex())
               .append("\nState: ")
               .append(instance.getState())
               .append("\nDisk Usage: ")
               .append(instance.getDiskUsage())
               .append("\nCPU: ")
               .append(instance.getCpu())
               .append("\nMemory Usage: ")
               .append(instance.getMemoryUsage())
               .append("\n"));
    executionLogCallback.saveExecutionLog(builder.toString());
  }

  public ApplicationDetail printApplicationDetail(
      ApplicationDetail applicationDetail, LogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("NAME: ")
                                              .append(applicationDetail.getName())
                                              .append("\nINSTANCE-COUNT: ")
                                              .append(applicationDetail.getInstances())
                                              .append("\nROUTES: ")
                                              .append(applicationDetail.getUrls())
                                              .append("\n")
                                              .toString());
    return applicationDetail;
  }

  /**
   * e.g. Downsize by 5,
   * Find out previous apps with non zero instances.
   * Process apps in descending order of versions.
   * keep processing till total counts taken down become 5
   * e.g. app_serv_env__5 is new app created,
   * app_serv_env__4   : 3
   * app_serv_env__3   : 3
   * app_serv_env__2   : 1
   * <p>
   * After this method, it will be
   * app_serv_env__4   : 0
   * app_serv_env__3   : 1
   * app_serv_env__2   : 1
   */
  public void downsizePreviousReleases(CfCommandDeployRequest cfCommandDeployRequest, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, List<CfServiceData> cfServiceDataUpdated, Integer updateCount,
      List<CfInternalInstanceElement> pcfInstanceElements, CfAppAutoscalarRequestData appAutoscalarRequestData)
      throws PivotalClientApiException {
    if (cfCommandDeployRequest.isStandardBlueGreen()) {
      executionLogCallback.saveExecutionLog("# BG Deployment. Old Application will not be downsized.");
      return;
    }

    executionLogCallback.saveExecutionLog("# Downsizing previous application version/s");

    CfAppSetupTimeDetails downsizeAppDetails = cfCommandDeployRequest.getDownsizeAppDetail();
    if (downsizeAppDetails == null) {
      executionLogCallback.saveExecutionLog("# No Application is available for downsize");
      return;
    }

    cfRequestConfig.setApplicationName(downsizeAppDetails.getApplicationName());
    ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(cfRequestConfig);
    executionLogCallback.saveExecutionLog(new StringBuilder()
                                              .append("APPLICATION-NAME: ")
                                              .append(applicationDetail.getName())
                                              .append("\nCURRENT-INSTANCE-COUNT: ")
                                              .append(applicationDetail.getInstances())
                                              .append("\nDESIRED-INSTANCE-COUNT: ")
                                              .append(updateCount)
                                              .toString());

    CfServiceData cfServiceData = CfServiceData.builder()
                                      .name(applicationDetail.getName())
                                      .id(applicationDetail.getId())
                                      .previousCount(applicationDetail.getInstances())
                                      .desiredCount(updateCount)
                                      .build();

    cfServiceDataUpdated.add(cfServiceData);

    if (updateCount >= applicationDetail.getInstances()) {
      executionLogCallback.saveExecutionLog("# No Downsize was required.\n");
      return;
    }

    // First disable App Auto scalar if attached with application
    if (cfCommandDeployRequest.isUseAppAutoscalar()) {
      appAutoscalarRequestData.setApplicationName(applicationDetail.getName());
      appAutoscalarRequestData.setApplicationGuid(applicationDetail.getId());
      appAutoscalarRequestData.setExpectedEnabled(true);
      boolean autoscalarStateChanged = disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
      cfServiceData.setDisableAutoscalarPerformed(autoscalarStateChanged);
    }

    ApplicationDetail applicationDetailAfterResize =
        downSize(cfServiceData, executionLogCallback, cfRequestConfig, pcfDeploymentManager);

    // Application that is downsized
    if (EmptyPredicate.isNotEmpty(applicationDetailAfterResize.getInstanceDetails())) {
      applicationDetailAfterResize.getInstanceDetails().forEach(instance
          -> pcfInstanceElements.add(CfInternalInstanceElement.builder()
                                         .applicationId(applicationDetailAfterResize.getId())
                                         .displayName(applicationDetailAfterResize.getName())
                                         .instanceIndex(instance.getIndex())
                                         .isUpsize(false)
                                         .build()));
    }
  }

  public void upsizeListOfInstances(LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      List<CfServiceData> cfServiceDataUpdated, CfRequestConfig cfRequestConfig, List<CfServiceData> upsizeList,
      List<CfInternalInstanceElement> pcfInstanceElements) throws PivotalClientApiException {
    if (isEmpty(upsizeList)) {
      executionLogCallback.saveExecutionLog("No application To Upsize");
      return;
    }

    for (CfServiceData cfServiceData : upsizeList) {
      executionLogCallback.saveExecutionLog(color("# Upsizing application:", White, Bold));
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(cfServiceData.getName())
                                                .append("\n" + CURRENT_INSTANCE_COUNT)
                                                .append(cfServiceData.getPreviousCount())
                                                .append("\n" + DESIRED_INSTANCE_COUNT)
                                                .append(cfServiceData.getDesiredCount())
                                                .toString());
      cfRequestConfig.setApplicationName(cfServiceData.getName());
      cfRequestConfig.setDesiredCount(cfServiceData.getDesiredCount());
      upsizeInstance(
          cfRequestConfig, pcfDeploymentManager, executionLogCallback, cfServiceDataUpdated, pcfInstanceElements);
      cfServiceDataUpdated.add(cfServiceData);
    }
  }

  public void downSizeListOfInstances(LogCallback executionLogCallback, List<CfServiceData> cfServiceDataUpdated,
      CfRequestConfig cfRequestConfig, List<CfServiceData> downSizeList,
      CfCommandRollbackRequest commandRollbackRequest, CfAppAutoscalarRequestData appAutoscalarRequestData)
      throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog("\n");
    for (CfServiceData cfServiceData : downSizeList) {
      executionLogCallback.saveExecutionLog(color("# Downsizing application:", White, Bold));
      executionLogCallback.saveExecutionLog(new StringBuilder()
                                                .append("\nAPPLICATION-NAME: ")
                                                .append(cfServiceData.getName())
                                                .append("\n" + CURRENT_INSTANCE_COUNT)
                                                .append(cfServiceData.getPreviousCount())
                                                .append("\n" + DESIRED_INSTANCE_COUNT)
                                                .append(cfServiceData.getDesiredCount())
                                                .toString());

      cfRequestConfig.setApplicationName(cfServiceData.getName());
      cfRequestConfig.setDesiredCount(cfServiceData.getDesiredCount());

      if (commandRollbackRequest.isUseAppAutoscalar()) {
        ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(cfRequestConfig);
        appAutoscalarRequestData.setApplicationName(applicationDetail.getName());
        appAutoscalarRequestData.setApplicationGuid(applicationDetail.getId());
        appAutoscalarRequestData.setExpectedEnabled(true);
        disableAutoscalar(appAutoscalarRequestData, executionLogCallback);
      }

      downSize(cfServiceData, executionLogCallback, cfRequestConfig, pcfDeploymentManager);

      cfServiceDataUpdated.add(cfServiceData);
    }
  }

  public void mapRouteMaps(String applicationName, List<String> routes, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Adding Routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationName);
    executionLogCallback.saveExecutionLog("ROUTE: \n[" + getRouteString(routes));
    // map
    cfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.mapRouteMapForApplication(cfRequestConfig, routes, executionLogCallback);
  }

  public void unmapRouteMaps(String applicationName, List<String> routes, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    executionLogCallback.saveExecutionLog(color("\n# Unmapping Routes", White, Bold));
    executionLogCallback.saveExecutionLog(APPLICATION + applicationName);
    executionLogCallback.saveExecutionLog("ROUTES: \n[" + getRouteString(routes));
    // unmap
    cfRequestConfig.setApplicationName(applicationName);
    pcfDeploymentManager.unmapRouteMapForApplication(cfRequestConfig, routes, executionLogCallback);
    executionLogCallback.saveExecutionLog("# Unmapping Routes was successfully completed");
  }

  public void printFileNamesInExecutionLogs(List<String> filePathList, LogCallback executionLogCallback) {
    if (EmptyPredicate.isEmpty(filePathList)) {
      return;
    }

    StringBuilder sb = new StringBuilder(1024);
    filePathList.forEach(filePath -> sb.append(color(format("- %s", filePath), Gray)).append(System.lineSeparator()));

    executionLogCallback.saveExecutionLog(sb.toString());
  }

  public String getCfCliPathOnDelegate(boolean useCli, CfCliVersion version) {
    if (!useCli) {
      return null;
    }

    if (version == null) {
      throw new InvalidArgumentsException("Requested CF CLI version on delegate cannot be null");
    }

    return cfCliDelegateResolver.getAvailableCfCliPathOnDelegate(version).orElseThrow(
        ()
            -> new InvalidArgumentsException(
                format("Unable to find CF CLI version on delegate, requested version: %s", version)));
  }

  public File generateWorkingDirectoryForDeployment() throws IOException {
    String workingDirecotry = UUIDGenerator.generateUuid();
    createDirectoryIfDoesNotExist(REPOSITORY_DIR_PATH);
    createDirectoryIfDoesNotExist(PCF_ARTIFACT_DOWNLOAD_DIR_PATH);
    String workingDir = PCF_ARTIFACT_DOWNLOAD_DIR_PATH + "/" + workingDirecotry;
    createDirectoryIfDoesNotExist(workingDir);
    return new File(workingDir);
  }

  public ApplicationDetail getNewlyCreatedApplication(CfRequestConfig cfRequestConfig,
      CfCommandDeployRequest cfCommandDeployRequest, CfDeploymentManager pcfDeploymentManager)
      throws PivotalClientApiException {
    cfRequestConfig.setApplicationName(cfCommandDeployRequest.getNewReleaseName());
    cfRequestConfig.setDesiredCount(cfCommandDeployRequest.getUpdateCount());
    return pcfDeploymentManager.getApplicationByName(cfRequestConfig);
  }

  @VisibleForTesting
  ApplicationDetail downSize(CfServiceData cfServiceData, LogCallback executionLogCallback,
      CfRequestConfig cfRequestConfig, CfDeploymentManager pcfDeploymentManager) throws PivotalClientApiException {
    cfRequestConfig.setApplicationName(cfServiceData.getName());
    cfRequestConfig.setDesiredCount(cfServiceData.getDesiredCount());

    ApplicationDetail applicationDetail = pcfDeploymentManager.resizeApplication(cfRequestConfig);

    executionLogCallback.saveExecutionLog("# Downsizing successful");
    executionLogCallback.saveExecutionLog("\n# App details after downsize:");
    printApplicationDetail(applicationDetail, executionLogCallback);
    return applicationDetail;
  }

  public boolean disableAutoscalar(CfAppAutoscalarRequestData pcfAppAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    return pcfDeploymentManager.changeAutoscalarState(pcfAppAutoscalarRequestData, executionLogCallback, false);
  }

  private List<InstanceDetail> filterNewUpsizedAppInstances(
      ApplicationDetail appDetailsBeforeUpsize, ApplicationDetail appDetailsAfterUpsize) {
    if (isEmpty(appDetailsBeforeUpsize.getInstanceDetails()) || isEmpty(appDetailsAfterUpsize.getInstanceDetails())) {
      return appDetailsAfterUpsize.getInstanceDetails();
    }

    List<String> alreadyUpsizedInstances =
        appDetailsBeforeUpsize.getInstanceDetails().stream().map(InstanceDetail::getIndex).collect(toList());

    return appDetailsAfterUpsize.getInstanceDetails()
        .stream()
        .filter(instanceDetail -> !alreadyUpsizedInstances.contains(instanceDetail.getIndex()))
        .collect(Collectors.toList());
  }

  @NotNull
  private File writeToManifestFile(String content, File manifestFile) throws IOException {
    if (!manifestFile.createNewFile()) {
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", "Failed to create file " + manifestFile.getCanonicalPath());
    }

    FileUtils.writeStringToFile(manifestFile, content, UTF_8);
    return manifestFile;
  }

  private String getRouteString(List<String> routeMaps) {
    if (EmptyPredicate.isEmpty(routeMaps)) {
      return StringUtils.EMPTY;
    }

    StringBuilder builder = new StringBuilder();
    routeMaps.forEach(routeMap -> builder.append("\n").append(routeMap));
    builder.append("\n]");
    return builder.toString();
  }

  public int getRevisionFromReleaseName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  /**
   * Returns Application that will be downsized in deployment process
   */
  public List<CfAppSetupTimeDetails> generateDownsizeDetails(ApplicationSummary activeApplicationSumamry) {
    List<CfAppSetupTimeDetails> downSizeUpdate = new ArrayList<>();
    if (activeApplicationSumamry != null) {
      List<String> urls = new ArrayList<>();
      urls.addAll(activeApplicationSumamry.getUrls());
      downSizeUpdate.add(CfAppSetupTimeDetails.builder()
                             .applicationGuid(activeApplicationSumamry.getId())
                             .applicationName(activeApplicationSumamry.getName())
                             .urls(urls)
                             .initialInstanceCount(activeApplicationSumamry.getInstances())
                             .build());
    }

    return downSizeUpdate;
  }

  public ApplicationSummary findCurrentActiveApplication(List<ApplicationSummary> previousReleases,
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    if (isEmpty(previousReleases)) {
      return null;
    }

    // For existing
    ApplicationSummary activeApplication = previousReleases.get(previousReleases.size() - 1);
    List<ApplicationSummary> activeVersions = new ArrayList<>();
    for (int i = previousReleases.size() - 1; i >= 0; i--) {
      ApplicationSummary applicationSummary = previousReleases.get(i);
      cfRequestConfig.setApplicationName(applicationSummary.getName());

      if (pcfDeploymentManager.isActiveApplication(cfRequestConfig, executionLogCallback)) {
        activeApplication = applicationSummary;
        activeVersions.add(applicationSummary);
      }
    }

    if (isNotEmpty(activeVersions) && activeVersions.size() > 1) {
      StringBuilder msgBuilder =
          new StringBuilder(256)
              .append("Invalid PCF Deployment State. Found Multiple applications having Env variable as ")
              .append(HARNESS__STATUS__IDENTIFIER)
              .append(
                  ": ACTIVE' identifier. Cant Determine actual active version.\n Only 1 is expected to have this Status. Active versions found are: \n");
      activeVersions.forEach(activeVersion -> msgBuilder.append(activeVersion.getName()).append(' '));
      executionLogCallback.saveExecutionLog(msgBuilder.toString(), ERROR);
      throw new InvalidPcfStateException(msgBuilder.toString(), INVALID_INFRA_STATE, USER_SRE);
    }

    return activeApplication;
  }

  public File createManifestYamlFileLocally(CfCreateApplicationRequestData requestData) throws IOException {
    File manifestFile = getManifestFile(requestData);
    return writeToManifestFile(requestData.getFinalManifestYaml(), manifestFile);
  }

  public File getManifestFile(CfCreateApplicationRequestData requestData) {
    return new File(requestData.getConfigPathVar() + "/" + requestData.getNewReleaseName() + ".yml");
  }

  public File createManifestVarsYamlFileLocally(
      CfCreateApplicationRequestData requestData, String varsContent, int index) {
    try {
      if (isBlank(varsContent)) {
        return null;
      }

      File manifestFile = getManifestVarsFile(requestData, index);
      return writeToManifestFile(varsContent, manifestFile);
    } catch (IOException e) {
      throw new UnexpectedException("Failed while writting manifest file on disk", e);
    }
  }

  public File getManifestVarsFile(CfCreateApplicationRequestData requestData, int index) {
    return new File(new StringBuilder(128)
                        .append(requestData.getConfigPathVar())
                        .append('/')
                        .append(requestData.getNewReleaseName())
                        .append("_vars_")
                        .append(index)
                        .append(".yml")
                        .toString());
  }

  public void deleteCreatedFile(List<File> files) {
    files.forEach(File::delete);
  }

  public String generateFinalManifestFilePath(String path) {
    return path.replace(".yml", "_1.yml");
  }

  public Map<String, Object> generateFinalMapForYamlDump(TreeMap<String, Object> applicationToBeUpdated) {
    Map<String, Object> yamlMap = new LinkedHashMap<>();

    addToMapIfExists(yamlMap, applicationToBeUpdated, NAME_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, MEMORY_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, INSTANCE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, BUILDPACK_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, BUILDPACKS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, PATH_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, COMMAND_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DISK_QUOTA_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DOCKER_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, DOMAINS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ENV_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HEALTH_CHECK_HTTP_ENDPOINT_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HEALTH_CHECK_TYPE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, "health-check-invocation-timeout");
    addToMapIfExists(yamlMap, applicationToBeUpdated, HOSTS_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, HOST_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, NO_HOSTNAME_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, NO_ROUTE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, RANDOM_ROUTE_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ROUTE_PATH_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, ROUTES_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, SERVICES_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, STACK_MANIFEST_YML_ELEMENT);
    addToMapIfExists(yamlMap, applicationToBeUpdated, TIMEOUT_MANIFEST_YML_ELEMENT);

    return yamlMap;
  }

  private void addToMapIfExists(Map destMap, Map sourceMap, String element) {
    if (sourceMap.containsKey(element)) {
      destMap.put(element, sourceMap.get(element));
    }
  }

  @VisibleForTesting
  public void handleManifestWithNoRoute(Map applicationToBeUpdated, boolean isBlueGreen) {
    // No route is not allowed for BG
    if (isBlueGreen) {
      throw new InvalidRequestException("Invalid Config. \"no-route\" can not be used with BG deployment");
    }

    // If no-route = true, then route element is not needed.
    applicationToBeUpdated.remove(ROUTES_MANIFEST_YML_ELEMENT);
  }

  public boolean shouldUseRandomRoute(Map applicationToBeUpdated, List<String> routeMaps) {
    return manifestContainsRandomRouteElement(applicationToBeUpdated) || isEmpty(routeMaps);
  }

  private boolean manifestContainsRandomRouteElement(Map applicationToBeUpdated) {
    return applicationToBeUpdated.containsKey(RANDOM_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) applicationToBeUpdated.get(RANDOM_ROUTE_MANIFEST_YML_ELEMENT);
  }
}