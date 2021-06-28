package io.harness.delegate.cf;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.CfCommandUnitConstants.Downsize;
import static io.harness.pcf.CfCommandUnitConstants.Upsize;
import static io.harness.pcf.CfCommandUnitConstants.Wrapup;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.beans.pcf.CfInternalInstanceElement;
import io.harness.delegate.beans.pcf.CfServiceData;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfCommandRollbackRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfDeployCommandResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.Misc;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;

@NoArgsConstructor
@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
public class PcfRollbackCommandTaskHandler extends PcfCommandTaskHandler {
  @Override
  public CfCommandExecutionResponse executeTaskInternal(CfCommandRequest cfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ILogStreamingTaskClient logStreamingTaskClient,
      boolean isInstanceSync) {
    if (!(cfCommandRequest instanceof CfCommandRollbackRequest)) {
      throw new InvalidArgumentsException(Pair.of("CfCommandRequest", "Must be instance of CfCommandRollbackRequest"));
    }
    LogCallback executionLogCallback = logStreamingTaskClient.obtainLogCallback(Upsize);
    executionLogCallback.saveExecutionLog(color("--------- Starting Rollback deployment", White, Bold));
    List<CfServiceData> cfServiceDataUpdated = new ArrayList<>();
    CfDeployCommandResponse cfDeployCommandResponse =
        CfDeployCommandResponse.builder().pcfInstanceElements(new ArrayList<>()).build();

    CfCommandRollbackRequest commandRollbackRequest = (CfCommandRollbackRequest) cfCommandRequest;

    File workingDirectory = null;
    Exception exception = null;
    try {
      // This will be CF_HOME for any cli related operations
      workingDirectory = pcfCommandTaskBaseHelper.generateWorkingDirectoryForDeployment();

      CfInternalConfig pcfConfig = cfCommandRequest.getPcfConfig();
      secretDecryptionService.decrypt(pcfConfig, encryptedDataDetails, false);
      if (CollectionUtils.isEmpty(commandRollbackRequest.getInstanceData())) {
        commandRollbackRequest.setInstanceData(new ArrayList<>());
      }

      CfRequestConfig cfRequestConfig =
          CfRequestConfig.builder()
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .orgName(commandRollbackRequest.getOrganization())
              .spaceName(commandRollbackRequest.getSpace())
              .timeOutIntervalInMins(commandRollbackRequest.getTimeoutIntervalInMin() == null
                      ? 10
                      : commandRollbackRequest.getTimeoutIntervalInMin())
              .cfHomeDirPath(workingDirectory.getAbsolutePath())
              .useCFCLI(commandRollbackRequest.isUseCfCLI())
              .cfCliPath(pcfCommandTaskBaseHelper.getCfCliPathOnDelegate(
                  cfCommandRequest.isUseCfCLI(), cfCommandRequest.getCfCliVersion()))
              .cfCliVersion(cfCommandRequest.getCfCliVersion())
              .limitPcfThreads(commandRollbackRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(commandRollbackRequest.isIgnorePcfConnectionContextCache())
              .build();

      // Will be used if app autoscalar is configured
      CfAppAutoscalarRequestData autoscalarRequestData =
          CfAppAutoscalarRequestData.builder()
              .cfRequestConfig(cfRequestConfig)
              .configPathVar(workingDirectory.getAbsolutePath())
              .timeoutInMins(commandRollbackRequest.getTimeoutIntervalInMin() != null
                      ? commandRollbackRequest.getTimeoutIntervalInMin()
                      : 10)
              .build();

      // get Upsize Instance data
      List<CfServiceData> upsizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(cfServiceData -> cfServiceData.getDesiredCount() > cfServiceData.getPreviousCount())
              .collect(toList());

      // get Downsize Instance data
      List<CfServiceData> downSizeList =
          commandRollbackRequest.getInstanceData()
              .stream()
              .filter(pcfServiceData -> pcfServiceData.getDesiredCount() < pcfServiceData.getPreviousCount())
              .collect(toList());

      List<CfInternalInstanceElement> pcfInstanceElements = new ArrayList<>();
      // During rollback, always upsize old ones
      pcfCommandTaskBaseHelper.upsizeListOfInstances(executionLogCallback, pcfDeploymentManager, cfServiceDataUpdated,
          cfRequestConfig, upsizeList, pcfInstanceElements);
      restoreRoutesForOldApplication(commandRollbackRequest, cfRequestConfig, executionLogCallback);
      // Enable autoscalar for older app, if it was disabled during deploy
      enableAutoscalarIfNeeded(upsizeList, autoscalarRequestData, executionLogCallback);
      executionLogCallback.saveExecutionLog("#---------- Upsize Application Successfully Completed", INFO, SUCCESS);

      executionLogCallback = logStreamingTaskClient.obtainLogCallback(Downsize);
      pcfCommandTaskBaseHelper.downSizeListOfInstances(executionLogCallback, cfServiceDataUpdated, cfRequestConfig,
          downSizeList, commandRollbackRequest, autoscalarRequestData);
      unmapRoutesFromNewAppAfterDownsize(executionLogCallback, commandRollbackRequest, cfRequestConfig);

      cfDeployCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      cfDeployCommandResponse.setOutput(StringUtils.EMPTY);
      cfDeployCommandResponse.setInstanceDataUpdated(cfServiceDataUpdated);
      cfDeployCommandResponse.getPcfInstanceElements().addAll(pcfInstanceElements);
      executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback completed successfully", INFO, SUCCESS);
    } catch (IOException | PivotalClientApiException e) {
      exception = e;
      logExceptionMessage(executionLogCallback, commandRollbackRequest, exception);
    } catch (Exception ex) {
      exception = ex;
      logExceptionMessage(executionLogCallback, commandRollbackRequest, exception);
    } finally {
      executionLogCallback = logStreamingTaskClient.obtainLogCallback(Wrapup);
      executionLogCallback.saveExecutionLog("#------- Deleting Temporary Files");
      if (workingDirectory != null) {
        try {
          FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory.getAbsolutePath());
          executionLogCallback.saveExecutionLog("Temporary Files Successfully deleted", INFO, SUCCESS);
        } catch (IOException e) {
          log.warn("Failed to delete temp cf home folder", e);
        }
      }
    }

    if (exception != null) {
      cfDeployCommandResponse.setCommandExecutionStatus(FAILURE);
      cfDeployCommandResponse.setInstanceDataUpdated(cfServiceDataUpdated);
      cfDeployCommandResponse.setOutput(ExceptionUtils.getMessage(exception));
    }

    return CfCommandExecutionResponse.builder()
        .commandExecutionStatus(cfDeployCommandResponse.getCommandExecutionStatus())
        .errorMessage(cfDeployCommandResponse.getOutput())
        .pcfCommandResponse(cfDeployCommandResponse)
        .build();
  }

  private void logExceptionMessage(
      LogCallback executionLogCallback, CfCommandRollbackRequest commandRollbackRequest, Exception exception) {
    log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception in processing PCF Rollback task [{}]",
        commandRollbackRequest, exception);
    executionLogCallback.saveExecutionLog("\n\n--------- PCF Rollback failed to complete successfully", ERROR, FAILURE);
    Misc.logAllMessages(exception, executionLogCallback);
  }

  @VisibleForTesting
  void enableAutoscalarIfNeeded(List<CfServiceData> upsizeList, CfAppAutoscalarRequestData autoscalarRequestData,
      LogCallback logCallback) throws PivotalClientApiException {
    for (CfServiceData cfServiceData : upsizeList) {
      if (!cfServiceData.isDisableAutoscalarPerformed()) {
        continue;
      }

      autoscalarRequestData.setApplicationName(cfServiceData.getName());
      autoscalarRequestData.setApplicationGuid(cfServiceData.getId());
      autoscalarRequestData.setExpectedEnabled(false);
      pcfDeploymentManager.changeAutoscalarState(autoscalarRequestData, logCallback, true);
    }
  }

  /**
   * This is for non BG deployment.
   * Older app will be mapped to routes it was originally mapped to.
   * In deploy state, once older app is downsized to 0, we remove routeMaps,
   * this step will restore them.
   */
  @VisibleForTesting
  void restoreRoutesForOldApplication(CfCommandRollbackRequest commandRollbackRequest, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    if (commandRollbackRequest.isStandardBlueGreenWorkflow()
        || EmptyPredicate.isEmpty(commandRollbackRequest.getAppsToBeDownSized())) {
      return;
    }

    CfAppSetupTimeDetails cfAppSetupTimeDetails = commandRollbackRequest.getAppsToBeDownSized().get(0);

    if (cfAppSetupTimeDetails != null) {
      cfRequestConfig.setApplicationName(cfAppSetupTimeDetails.getApplicationName());
      ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(cfRequestConfig);

      if (EmptyPredicate.isEmpty(cfAppSetupTimeDetails.getUrls())) {
        return;
      }

      if (EmptyPredicate.isEmpty(applicationDetail.getUrls())
          || !cfAppSetupTimeDetails.getUrls().containsAll(applicationDetail.getUrls())) {
        pcfCommandTaskBaseHelper.mapRouteMaps(cfAppSetupTimeDetails.getApplicationName(),
            cfAppSetupTimeDetails.getUrls(), cfRequestConfig, executionLogCallback);
      }
    }
  }

  @VisibleForTesting
  void unmapRoutesFromNewAppAfterDownsize(LogCallback executionLogCallback,
      CfCommandRollbackRequest commandRollbackRequest, CfRequestConfig cfRequestConfig)
      throws PivotalClientApiException {
    if (commandRollbackRequest.isStandardBlueGreenWorkflow()
        || commandRollbackRequest.getNewApplicationDetails() == null
        || isBlank(commandRollbackRequest.getNewApplicationDetails().getApplicationName())) {
      return;
    }

    cfRequestConfig.setApplicationName(commandRollbackRequest.getNewApplicationDetails().getApplicationName());
    ApplicationDetail appDetail = pcfDeploymentManager.getApplicationByName(cfRequestConfig);

    if (appDetail.getInstances() == 0) {
      pcfCommandTaskBaseHelper.unmapExistingRouteMaps(appDetail, cfRequestConfig, executionLogCallback);
    }
  }
}