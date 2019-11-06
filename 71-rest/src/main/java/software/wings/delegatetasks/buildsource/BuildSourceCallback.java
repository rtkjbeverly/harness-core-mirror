package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.STABLE;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.UNSTABLE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PermitService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@Slf4j
public class BuildSourceCallback implements NotifyCallback {
  private String accountId;
  private String artifactStreamId;
  private String permitId;
  private String settingId;
  private List<BuildDetails> builds;
  private static final int MAX_ARTIFACTS_COLLECTION_FOR_WARN = 100;
  private static final int MAX_LOGS = 2000;

  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient TriggerService triggerService;
  @Inject private transient DeploymentTriggerService deploymentTriggerService;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient PermitService permitService;
  @Inject private transient AlertService alertService;
  @Inject private transient ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject @Named("BuildSourceCallbackExecutor") private ExecutorService executorService;

  // Used in tests.
  public BuildSourceCallback() {}

  public BuildSourceCallback(
      String accountId, String artifactStreamId, String permitId, String settingId) { // todo: new constr with settingId
    this.accountId = accountId;
    this.artifactStreamId = artifactStreamId;
    this.permitId = permitId;
    this.settingId = settingId;
  }

  private void handleResponseForSuccess(ResponseData notifyResponseData, ArtifactStream artifactStream) {
    try {
      executorService.submit(() -> {
        try {
          handleResponseForSuccessInternal(notifyResponseData, artifactStream);
        } catch (Exception ex) {
          logger.error(
              "Error while processing response for BuildSourceCallback for accountId:[{}] artifactStreamId:[{}] permitId:[{}] settingId:[{}]",
              accountId, artifactStreamId, permitId, settingId, ex);
        }
      });
    } catch (RejectedExecutionException ex) {
      logger.error(
          "RejectedExecutionException for BuildSourceCallback for accountId:[{}] artifactStreamId:[{}] permitId:[{}] settingId:[{}]",
          accountId, artifactStreamId, permitId, settingId, ex);
    }
  }

  @VisibleForTesting
  void handleResponseForSuccessInternal(ResponseData notifyResponseData, ArtifactStream artifactStream) {
    logger.info(
        "Processing response for BuildSourceCallback for accountId:[{}] artifactStreamId:[{}] permitId:[{}] settingId:[{}]",
        accountId, artifactStreamId, permitId, settingId);

    BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
    BuildSourceResponse buildSourceResponse = buildSourceExecutionResponse.getBuildSourceResponse();
    if (buildSourceResponse != null) {
      builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
    } else {
      logger.warn(
          "ASYNC_ARTIFACT_COLLECTION: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
          buildSourceExecutionResponse, artifactStreamId);
      updatePermit(artifactStream, false);
      return;
    }

    // NOTE: buildSourceResponse is not null at this point.
    try {
      boolean isUnstable = UNSTABLE.name().equals(artifactStream.getCollectionStatus());
      if (isUnstable && buildSourceResponse.isStable()) {
        // If the artifact stream is unstable and buildSourceResponse has stable as true, mark this artifact
        // stream as stable.
        artifactStreamService.updateCollectionStatus(accountId, artifactStreamId, STABLE.name());
      }
      List<Artifact> artifacts = processBuilds(artifactStream);

      if (artifacts.size() > MAX_ARTIFACTS_COLLECTION_FOR_WARN) {
        logger.warn("Collected {} artifacts in single collection", artifacts.size());
      }
      if (isNotEmpty(artifacts)) {
        artifacts.stream().limit(MAX_LOGS).forEach(artifact -> {
          logger.info("Build number {} new artifacts collected for artifactStreamId {}", artifact.getBuildNo(),
              artifactStream.getUuid());
        });

        if (isUnstable) {
          updatePermit(artifactStream, false);
          return;
        }

        // Invoke triggers for only stable artifact streams.
        if (!featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, accountId)) {
          triggerService.triggerExecutionPostArtifactCollectionAsync(
              accountId, artifactStream.fetchAppId(), artifactStreamId, artifacts);
        } else {
          deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
              accountId, artifactStream.fetchAppId(), artifactStreamId, artifacts);
        }
      }

      updatePermit(artifactStream, false);
    } catch (WingsException ex) {
      ex.addContext(Account.class, accountId);
      ex.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
      updatePermit(artifactStream, true);
    }
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    logger.info("In notify for artifact stream id: [{}]", artifactStreamId);
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(
             artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
      if (notifyResponseData instanceof BuildSourceExecutionResponse) {
        if (SUCCESS.equals(((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus())) {
          handleResponseForSuccess(notifyResponseData, artifactStream);
        } else {
          logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
              ((BuildSourceExecutionResponse) notifyResponseData).getErrorMessage());
          //        permitService.releasePermit(permitId, true);
          updatePermit(artifactStream, true);
        }
      } else {
        notifyError(response);
      }
    }
  }

  private void updatePermit(ArtifactStream artifactStream, boolean failed) {
    if (failed) {
      int failedCronAttempts = artifactStream.getFailedCronAttempts() + 1;
      artifactStreamService.updateFailedCronAttempts(
          artifactStream.getAccountId(), artifactStream.getUuid(), failedCronAttempts);
      logger.warn(
          "ASYNC_ARTIFACT_COLLECTION: failed to fetch/process builds for artifactStream[{}], totalFailedAttempt:[{}]",
          artifactStreamId, failedCronAttempts);
      if (PermitServiceImpl.shouldSendAlert(failedCronAttempts)) {
        String appId = artifactStream.fetchAppId();
        if (!GLOBAL_APP_ID.equals(appId)) {
          alertService.openAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
              ArtifactCollectionFailedAlert.builder()
                  .appId(appId)
                  .serviceId(artifactStream.getServiceId())
                  .artifactStreamId(artifactStreamId)
                  .build());
        } else {
          alertService.openAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
              ArtifactCollectionFailedAlert.builder()
                  .settingId(artifactStream.getSettingId())
                  .artifactStreamId(artifactStreamId)
                  .build());
        }
      }
    } else {
      if (artifactStream.getFailedCronAttempts() != 0) {
        logger.warn("ASYNC_ARTIFACT_COLLECTION: successfully fetched builds after [{}] failures for artifactStream[{}]",
            artifactStream.getFailedCronAttempts(), artifactStreamId);
        artifactStreamService.updateFailedCronAttempts(artifactStream.getAccountId(), artifactStream.getUuid(), 0);
        permitService.releasePermitByKey(artifactStream.getUuid());
        alertService.closeAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
            ArtifactCollectionFailedAlert.builder().artifactStreamId(artifactStreamId).build());
      }
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
          ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      logger.error("Unexpected  notify response:[{}] during artifact collection for artifactStreamId {} ", response,
          artifactStreamId);
    }
    updatePermit(artifactStream, true);
  }

  private List<Artifact> processBuilds(ArtifactStream artifactStream) {
    if (isEmpty(builds)) {
      return new ArrayList<>();
    }
    if (artifactStream == null) {
      logger.info("Artifact stream: [{}] does not exist. Returning", artifactStreamId);
      return new ArrayList<>();
    }

    // New build are filtered at the delegate. So all the builds coming in the BuildSourceExecutionResponse are the ones
    // not present in the DB.
    return builds.stream()
        .map(buildDetails -> artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, buildDetails)))
        .collect(Collectors.toList());
  }
}
