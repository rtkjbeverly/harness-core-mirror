package io.harness.delegate.cf;

import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.IVAN;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.task.pcf.exception.InvalidPcfStateException;
import io.harness.delegate.task.pcf.request.CfCommandDeployRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfCliDelegateResolver;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDP)
public class PcfCommandTaskBaseHelperTest extends CategoryTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String RUNNING = "RUNNING";
  public static final String APP_ID = "APP_ID";
  public static final String ACTIVITY_ID = "ACTIVITY_ID";
  public static final String MANIFEST_YAML = "  applications:\n"
      + "  - name: ${APPLICATION_NAME}\n"
      + "    memory: 350M\n"
      + "    instances: ${INSTANCE_COUNT}\n"
      + "    buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "    path: ${FILE_LOCATION}\n"
      + "    routes:\n"
      + "      - route: ${ROUTE_MAP}\n";

  public static final String MANIFEST_YAML_LOCAL_RESOLVED = "---\n"
      + "applications:\n"
      + "- name: app1__1\n"
      + "  memory: 350M\n"
      + "  instances: 0\n"
      + "  buildpack: https://github.com/cloudfoundry/java-buildpack.git\n"
      + "  path: /root/app\n"
      + "  routes:\n"
      + "  - route: app.harness.io\n"
      + "  - route: stage.harness.io\n";

  private static final String RELEASE_NAME = "name"
      + "_pcfCommandHelperTest";

  @Mock CfDeploymentManager pcfDeploymentManager;
  @Mock LogCallback executionLogCallback;
  @Mock CfCliDelegateResolver cfCliDelegateResolver;
  @InjectMocks @Spy PcfCommandTaskBaseHelper pcfCommandTaskHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateManifestYamlFileLocally() throws Exception {
    File file = null;

    try {
      file = pcfCommandTaskHelper.createManifestYamlFileLocally(
          CfCreateApplicationRequestData.builder()
              .finalManifestYaml(MANIFEST_YAML_LOCAL_RESOLVED)
              .password("ABCD".toCharArray())
              .configPathVar(".")
              .newReleaseName(RELEASE_NAME + System.currentTimeMillis())
              .build());

      assertThat(file.exists()).isTrue();

      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      String line;
      StringBuilder stringBuilder = new StringBuilder(128);
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line).append('\n');
      }

      assertThat(stringBuilder.toString()).isEqualTo(MANIFEST_YAML_LOCAL_RESOLVED);
      pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(file));
      assertThat(file.exists()).isFalse();
    } finally {
      if (file != null && file.exists()) {
        FileIo.deleteFileIfExists(file.getAbsolutePath());
      }
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRevisionFromReleaseName() throws Exception {
    Integer revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__1");
    assertThat(1 == revision).isTrue();

    revision = pcfCommandTaskHelper.getRevisionFromReleaseName("app_serv_env__2");
    assertThat(2 == revision).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateManifestVarsYamlFileLocally() throws Exception {
    CfCreateApplicationRequestData requestData = CfCreateApplicationRequestData.builder()
                                                     .configPathVar(".")
                                                     .newReleaseName("app" + System.currentTimeMillis())
                                                     .build();

    File f = pcfCommandTaskHelper.createManifestVarsYamlFileLocally(requestData, "a:b", 1);
    assertThat(f).isNotNull();

    BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
    String line;
    StringBuilder stringBuilder = new StringBuilder(128);
    while ((line = bufferedReader.readLine()) != null) {
      stringBuilder.append(line);
    }

    assertThat(stringBuilder.toString()).isEqualTo("a:b");
    pcfCommandTaskHelper.deleteCreatedFile(Arrays.asList(f));
    assertThat(f.exists()).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownSizeListOfInstances() throws Exception {
    reset(pcfDeploymentManager);
    ApplicationDetail detail = ApplicationDetail.builder()
                                   .diskQuota(1)
                                   .id("id")
                                   .name("app")
                                   .instances(0)
                                   .memoryLimit(1)
                                   .stack("stack")
                                   .runningInstances(2)
                                   .requestedState("RUNNING")
                                   .build();

    doReturn(detail).when(pcfDeploymentManager).getApplicationByName(any());
    doReturn(detail).when(pcfDeploymentManager).resizeApplication(any());

    List<CfServiceData> cfServiceDataListToBeUpdated = new ArrayList<>();
    List<CfServiceData> cfServiceDataList = new ArrayList<>();
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    cfServiceDataList.add(CfServiceData.builder().name("test").desiredCount(2).build());
    CfCommandRollbackRequest commandRollbackRequest = CfCommandRollbackRequest.builder().useAppAutoscalar(true).build();
    String path = EMPTY;

    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfCommandTaskHelper.downSizeListOfInstances(executionLogCallback, cfServiceDataListToBeUpdated, cfRequestConfig,
        cfServiceDataList, commandRollbackRequest,
        CfAppAutoscalarRequestData.builder().applicationName(detail.getName()).applicationGuid(detail.getId()).build());
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
    assertThat(cfServiceDataListToBeUpdated.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownsizePreviousReleases() throws Exception {
    CfCommandDeployRequest request =
        CfCommandDeployRequest.builder().accountId(ACCOUNT_ID).downsizeAppDetail(null).build();

    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    List<CfServiceData> cfServiceDataList = new ArrayList<>();
    List<CfInternalInstanceElement> pcfInstanceElements = new ArrayList<>();

    // No old app exists
    pcfCommandTaskHelper.downsizePreviousReleases(request, cfRequestConfig, executionLogCallback, cfServiceDataList, 0,
        pcfInstanceElements, CfAppAutoscalarRequestData.builder().build());
    verify(pcfDeploymentManager, never()).getApplicationByName(any());

    InstanceDetail instanceDetail0 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("0")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();

    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("1")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();
    // old app exists, but downsize is not required.
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .diskQuota(1)
                                              .id("id")
                                              .name("app")
                                              .instanceDetails(instanceDetail0, instanceDetail1)
                                              .instances(2)
                                              .memoryLimit(1)
                                              .stack("stack")
                                              .runningInstances(2)
                                              .requestedState("RUNNING")
                                              .build();

    ApplicationDetail applicationDetailAfterDownsize = ApplicationDetail.builder()
                                                           .diskQuota(1)
                                                           .id("id")
                                                           .name("app")
                                                           .instanceDetails(instanceDetail0)
                                                           .instances(1)
                                                           .memoryLimit(1)
                                                           .stack("stack")
                                                           .runningInstances(1)
                                                           .requestedState("RUNNING")
                                                           .build();

    request.setDownsizeAppDetail(
        CfAppSetupTimeDetails.builder().applicationGuid("1").applicationName("app").initialInstanceCount(1).build());
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());

    // For BG, downsize should never happen.
    request.setStandardBlueGreen(true);
    pcfCommandTaskHelper.downsizePreviousReleases(request, cfRequestConfig, executionLogCallback, cfServiceDataList, 2,
        pcfInstanceElements, CfAppAutoscalarRequestData.builder().build());
    verify(pcfDeploymentManager, never()).getApplicationByName(any());

    // exptectedCount = cuurrentCount, no downsize should be called.
    request.setStandardBlueGreen(false);
    pcfCommandTaskHelper.downsizePreviousReleases(request, cfRequestConfig, executionLogCallback, cfServiceDataList, 2,
        pcfInstanceElements, CfAppAutoscalarRequestData.builder().applicationGuid("id").applicationName("app").build());
    verify(pcfDeploymentManager, times(1)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, never()).downSize(any(), any(), any(), any());
    assertThat(cfServiceDataList.size()).isEqualTo(1);
    assertThat(cfServiceDataList.get(0).getDesiredCount()).isEqualTo(2);
    assertThat(cfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(cfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(cfServiceDataList.get(0).getName()).isEqualTo("app");

    // Downsize application from 2 to 1
    doReturn(applicationDetailAfterDownsize).when(pcfDeploymentManager).resizeApplication(any());
    pcfInstanceElements.clear();
    cfServiceDataList.clear();
    pcfCommandTaskHelper.downsizePreviousReleases(request, cfRequestConfig, executionLogCallback, cfServiceDataList, 1,
        pcfInstanceElements, CfAppAutoscalarRequestData.builder().build());
    verify(pcfDeploymentManager, times(2)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, times(1)).downSize(any(), any(), any(), any());
    assertThat(cfServiceDataList.size()).isEqualTo(1);
    assertThat(cfServiceDataList.get(0).getDesiredCount()).isEqualTo(1);
    assertThat(cfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(cfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(cfServiceDataList.get(0).getName()).isEqualTo("app");

    assertThat(pcfInstanceElements.size()).isEqualTo(1);
    assertThat(pcfInstanceElements.get(0).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(0).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("0");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testDownsizePreviousReleases_autoscalar() throws Exception {
    CfCommandDeployRequest request =
        CfCommandDeployRequest.builder().accountId(ACCOUNT_ID).downsizeAppDetail(null).useAppAutoscalar(true).build();

    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().build();
    List<CfServiceData> cfServiceDataList = new ArrayList<>();
    List<CfInternalInstanceElement> pcfInstanceElements = new ArrayList<>();

    InstanceDetail instanceDetail0 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("0")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();

    InstanceDetail instanceDetail1 = InstanceDetail.builder()
                                         .cpu(0.0)
                                         .index("1")
                                         .diskQuota(0l)
                                         .diskUsage(0l)
                                         .memoryQuota(0l)
                                         .memoryUsage(0l)
                                         .state("RUNNING")
                                         .build();
    // old app exists, but downsize is not required.
    ApplicationDetail applicationDetail = ApplicationDetail.builder()
                                              .diskQuota(1)
                                              .id("id")
                                              .name("app")
                                              .instanceDetails(instanceDetail0, instanceDetail1)
                                              .instances(2)
                                              .memoryLimit(1)
                                              .stack("stack")
                                              .runningInstances(2)
                                              .requestedState("RUNNING")
                                              .build();

    ApplicationDetail applicationDetailAfterDownsize = ApplicationDetail.builder()
                                                           .diskQuota(1)
                                                           .id("id")
                                                           .name("app")
                                                           .instanceDetails(instanceDetail0)
                                                           .instances(1)
                                                           .memoryLimit(1)
                                                           .stack("stack")
                                                           .runningInstances(1)
                                                           .requestedState("RUNNING")
                                                           .build();

    request.setDownsizeAppDetail(
        CfAppSetupTimeDetails.builder().applicationGuid("1").applicationName("app").initialInstanceCount(1).build());
    doReturn(applicationDetail).when(pcfDeploymentManager).getApplicationByName(any());

    // Downsize application from 2 to 1
    doReturn(applicationDetailAfterDownsize).when(pcfDeploymentManager).resizeApplication(any());
    doReturn(true).when(pcfDeploymentManager).changeAutoscalarState(any(), any(), anyBoolean());
    pcfInstanceElements.clear();
    cfServiceDataList.clear();
    pcfCommandTaskHelper.downsizePreviousReleases(request, cfRequestConfig, executionLogCallback, cfServiceDataList, 1,
        pcfInstanceElements,
        CfAppAutoscalarRequestData.builder()
            .applicationName(applicationDetail.getName())
            .applicationGuid(applicationDetail.getId())
            .build());
    verify(pcfDeploymentManager, times(1)).getApplicationByName(any());
    verify(pcfCommandTaskHelper, times(1)).downSize(any(), any(), any(), any());
    verify(pcfDeploymentManager, times(1)).changeAutoscalarState(any(), any(), anyBoolean());
    assertThat(cfServiceDataList.size()).isEqualTo(1);
    assertThat(cfServiceDataList.get(0).getDesiredCount()).isEqualTo(1);
    assertThat(cfServiceDataList.get(0).getPreviousCount()).isEqualTo(2);
    assertThat(cfServiceDataList.get(0).getId()).isEqualTo("id");
    assertThat(cfServiceDataList.get(0).getName()).isEqualTo("app");
    assertThat(cfServiceDataList.get(0).isDisableAutoscalarPerformed()).isTrue();

    assertThat(pcfInstanceElements.size()).isEqualTo(1);
    assertThat(pcfInstanceElements.get(0).getApplicationId()).isEqualTo("id");
    assertThat(pcfInstanceElements.get(0).getDisplayName()).isEqualTo("app");
    assertThat(pcfInstanceElements.get(0).getInstanceIndex()).isEqualTo("0");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleManifestWithNoRoute() {
    Map map = new HashMap<>();
    map.put(ROUTES_MANIFEST_YML_ELEMENT, new Object());
    pcfCommandTaskHelper.handleManifestWithNoRoute(map, false);
    assertThat(map.containsKey(ROUTES_MANIFEST_YML_ELEMENT)).isFalse();

    try {
      pcfCommandTaskHelper.handleManifestWithNoRoute(map, true);
      fail("Exception was expected, as no-route cant be used with BG");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Config. \"no-route\" can not be used with BG deployment");
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateYamlFileLocally() throws Exception {
    String data = "asd";
    File file = pcfCommandTaskHelper.createYamlFileLocally("./test" + System.currentTimeMillis(), data);
    assertThat(file.exists()).isTrue();
    String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    assertThat(content).isEqualTo(data);
    FileIo.deleteFileIfExists(file.getAbsolutePath());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateDownsizeDetails() {
    List<CfAppSetupTimeDetails> details =
        pcfCommandTaskHelper.generateDownsizeDetails(ApplicationSummary.builder()
                                                         .name("a_s_e__4")
                                                         .diskQuota(1)
                                                         .requestedState(RUNNING)
                                                         .id("1")
                                                         .urls(new String[] {"url1", "url2"})
                                                         .instances(2)
                                                         .memoryLimit(1)
                                                         .runningInstances(0)
                                                         .build());
    assertThat(details).isNotNull();
    assertThat(details.size()).isEqualTo(1);
    assertThat(details.get(0).getApplicationName()).isEqualTo("a_s_e__4");
    assertThat(details.get(0).getUrls()).containsExactly("url1", "url2");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFindCurrentActiveApplication() throws Exception {
    ApplicationSummary currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(null, null, null);
    assertThat(currentActiveApplication).isNull();

    doReturn(false).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases = Arrays.asList(ApplicationSummary.builder()
                                                                        .name("a_s_e__4")
                                                                        .diskQuota(1)
                                                                        .requestedState(RUNNING)
                                                                        .id("1")
                                                                        .urls(new String[] {"url1", "url2"})
                                                                        .instances(2)
                                                                        .memoryLimit(1)
                                                                        .runningInstances(0)
                                                                        .build(),
        ApplicationSummary.builder()
            .name("a_s_e__5")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url3", "url4"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(
        previousReleases, CfRequestConfig.builder().build(), executionLogCallback);
    assertThat(currentActiveApplication).isNotNull();
    assertThat(currentActiveApplication.getName()).isEqualTo("a_s_e__5");
    assertThat(currentActiveApplication.getUrls()).containsExactly("url3", "url4");

    doReturn(true).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases1 = Arrays.asList(ApplicationSummary.builder()
                                                                         .name("a_s_e__6")
                                                                         .diskQuota(1)
                                                                         .requestedState(RUNNING)
                                                                         .id("1")
                                                                         .urls(new String[] {"url5", "url6"})
                                                                         .instances(2)
                                                                         .memoryLimit(1)
                                                                         .runningInstances(0)
                                                                         .build(),
        ApplicationSummary.builder()
            .name("a_s_e__7")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url7", "url8"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    assertThatThrownBy(()
                           -> pcfCommandTaskHelper.findCurrentActiveApplication(
                               previousReleases1, CfRequestConfig.builder().build(), executionLogCallback))
        .isInstanceOf(InvalidPcfStateException.class);

    doReturn(false).doReturn(true).when(pcfDeploymentManager).isActiveApplication(any(), any());
    final List<ApplicationSummary> previousReleases2 = Arrays.asList(ApplicationSummary.builder()
                                                                         .name("a_s_e__6")
                                                                         .diskQuota(1)
                                                                         .requestedState(RUNNING)
                                                                         .id("1")
                                                                         .urls(new String[] {"url5", "url6"})
                                                                         .instances(2)
                                                                         .memoryLimit(1)
                                                                         .runningInstances(0)
                                                                         .build(),
        ApplicationSummary.builder()
            .name("a_s_e__7")
            .diskQuota(1)
            .requestedState(RUNNING)
            .id("1")
            .urls(new String[] {"url7", "url8"})
            .instances(2)
            .memoryLimit(1)
            .runningInstances(0)
            .build());

    currentActiveApplication = pcfCommandTaskHelper.findCurrentActiveApplication(
        previousReleases2, CfRequestConfig.builder().build(), executionLogCallback);
    assertThat(currentActiveApplication).isNotNull();
    assertThat(currentActiveApplication.getName()).isEqualTo("a_s_e__6");
    assertThat(currentActiveApplication.getUrls()).containsExactly("url5", "url6");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrintInstanceDetails() throws Exception {
    String output = "Instance Details:\n"
        + "Index: 0\n"
        + "State: RUNNING\n"
        + "Disk Usage: 1\n"
        + "CPU: 0.0\n"
        + "Memory Usage: 1\n"
        + "\n"
        + "Index: 1\n"
        + "State: RUNNING\n"
        + "Disk Usage: 2\n"
        + "CPU: 0.0\n"
        + "Memory Usage: 2\n";
    InstanceDetail detail0 = InstanceDetail.builder()
                                 .cpu(0.0)
                                 .index("0")
                                 .diskQuota(1l)
                                 .diskUsage(1l)
                                 .memoryQuota(1l)
                                 .memoryUsage(1l)
                                 .state("RUNNING")
                                 .build();

    InstanceDetail detail1 = InstanceDetail.builder()
                                 .cpu(0.0)
                                 .index("1")
                                 .diskQuota(2l)
                                 .diskUsage(2l)
                                 .memoryQuota(2l)
                                 .memoryUsage(2l)
                                 .state("RUNNING")
                                 .build();

    pcfCommandTaskHelper.printInstanceDetails(executionLogCallback, Arrays.asList(detail0, detail1));
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(executionLogCallback).saveExecutionLog(captor.capture());
    String val = captor.getValue();
    assertThat(output).isEqualTo(val);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCfCliPathOnDelegate() {
    String defaultCfPath = "cf";
    doReturn(Optional.of(defaultCfPath)).when(cfCliDelegateResolver).getAvailableCfCliPathOnDelegate(CfCliVersion.V6);
    String cfCliPathOnDelegate = pcfCommandTaskHelper.getCfCliPathOnDelegate(true, CfCliVersion.V6);

    assertThat(cfCliPathOnDelegate).isNotEmpty();
    assertThat(cfCliPathOnDelegate).isEqualTo(defaultCfPath);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCfCliPathOnDelegateWithNullVersion() {
    assertThatThrownBy(() -> pcfCommandTaskHelper.getCfCliPathOnDelegate(true, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Requested CF CLI version on delegate cannot be null");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetCfCliPathOnDelegateWithNotInstalledCliOnDelegate() {
    doReturn(Optional.empty()).when(cfCliDelegateResolver).getAvailableCfCliPathOnDelegate(CfCliVersion.V7);

    assertThatThrownBy(() -> pcfCommandTaskHelper.getCfCliPathOnDelegate(true, CfCliVersion.V7))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Unable to find CF CLI version on delegate, requested version: V7");
  }
}