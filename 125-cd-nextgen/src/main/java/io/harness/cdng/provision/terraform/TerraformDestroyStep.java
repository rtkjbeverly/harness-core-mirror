package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformCommand;
import io.harness.delegate.task.terraform.TerraformCommandUnit;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.provision.TerraformConstants;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class TerraformDestroyStep extends TaskExecutableWithRollback<TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_DESTROY.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private TerraformStepHelper helper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepElementParameters, StepInputPackage inputPackage) {
    TerraformDestroyStepParameters parameters = (TerraformDestroyStepParameters) stepElementParameters.getSpec();
    TerraformStepConfigurationType configurationType = parameters.getStepConfigurationType();
    switch (configurationType) {
      case INLINE:
        return obtainInlineTask(ambiance, parameters);
      case INHERIT_FROM_PLAN:
        return obtainInheritedTask(ambiance, parameters);
      case INHERIT_FROM_APPLY:
        return obtainLastApplyTask(ambiance, parameters);
      default:
        throw new InvalidRequestException(
            String.format("Unknown configuration Type: [%s]", configurationType.getDisplayName()));
    }
  }

  private TaskRequest obtainInlineTask(Ambiance ambiance, TerraformDestroyStepParameters parameters) {
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(parameters.getProvisionerIdentifier(), ambiance);
    builder.currentStateFileId(helper.getLatestFileId(entityId))
        .taskType(TFTaskType.DESTROY)
        .terraformCommand(TerraformCommand.DESTROY)
        .entityId(entityId)
        .workspace(ParameterFieldHelper.getParameterFieldValue(parameters.getWorkspace()))
        .configFile(helper.getGitFetchFilesConfig(
            parameters.getConfigFilesWrapper().getStoreConfig(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
        .inlineVarFiles(ParameterFieldHelper.getParameterFieldValue(parameters.getInlineVarFiles()));
    if (EmptyPredicate.isNotEmpty(parameters.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfigWrapper varFileWrapper : parameters.getRemoteVarFiles()) {
        varFilesConfig.add(helper.getGitFetchFilesConfig(
            varFileWrapper.getStoreConfig(), ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.backendConfig(ParameterFieldHelper.getParameterFieldValue(parameters.getBackendConfig()))
        .targets(ParameterFieldHelper.getParameterFieldValue(parameters.getTargets()))
        .saveTerraformStateJson(cdFeatureFlagHelper.isEnabled(accountId, FeatureName.EXPORT_TF_PLAN))
        .environmentVariables(helper.getEnvironmentVariablesMap(parameters.getEnvironmentVariables()));

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(parameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformCommandUnit.Destroy.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  private TaskRequest obtainInheritedTask(Ambiance ambiance, TerraformDestroyStepParameters parameters) {
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder()
                                                   .taskType(TFTaskType.DESTROY)
                                                   .entityId(parameters.getProvisionerIdentifier());
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(parameters.getProvisionerIdentifier(), ambiance);
    builder.entityId(entityId);
    builder.currentStateFileId(helper.getLatestFileId(entityId));
    TerraformInheritOutput inheritOutput =
        helper.getSavedInheritOutput(parameters.getProvisionerIdentifier(), ambiance);
    builder.workspace(inheritOutput.getWorkspace())
        .configFile(helper.getGitFetchFilesConfig(
            inheritOutput.getConfigFiles(), ambiance, TerraformStepHelper.TF_CONFIG_FILES));
    if (EmptyPredicate.isNotEmpty(inheritOutput.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfig storeConfig : inheritOutput.getRemoteVarFiles()) {
        varFilesConfig.add(
            helper.getGitFetchFilesConfig(storeConfig, ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.inlineVarFiles(inheritOutput.getInlineVarFiles())
        .backendConfig(inheritOutput.getBackendConfig())
        .targets(inheritOutput.getTargets())
        .saveTerraformStateJson(cdFeatureFlagHelper.isEnabled(accountId, FeatureName.EXPORT_TF_PLAN))
        .encryptionConfig(inheritOutput.getEncryptionConfig())
        .encryptedTfPlan(inheritOutput.getEncryptedTfPlan())
        .planName(inheritOutput.getPlanName())
        .environmentVariables(inheritOutput.getEnvironmentVariables());

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(parameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformCommandUnit.Destroy.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  private TaskRequest obtainLastApplyTask(Ambiance ambiance, TerraformDestroyStepParameters parameters) {
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder()
                                                   .taskType(TFTaskType.DESTROY)
                                                   .entityId(parameters.getProvisionerIdentifier());
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(parameters.getProvisionerIdentifier(), ambiance);
    builder.entityId(entityId);
    builder.currentStateFileId(helper.getLatestFileId(entityId));
    TerraformConfig terraformConfig = helper.getLastSuccessfulApplyConfig(parameters, ambiance);
    builder.workspace(terraformConfig.getWorkspace())
        .configFile(helper.getGitFetchFilesConfig(
            terraformConfig.getConfigFiles(), ambiance, TerraformStepHelper.TF_CONFIG_FILES));
    if (EmptyPredicate.isNotEmpty(terraformConfig.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfig storeConfig : terraformConfig.getRemoteVarFiles()) {
        varFilesConfig.add(
            helper.getGitFetchFilesConfig(storeConfig, ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.inlineVarFiles(terraformConfig.getInlineVarFiles())
        .backendConfig(terraformConfig.getBackendConfig())
        .targets(terraformConfig.getTargets())
        .saveTerraformStateJson(cdFeatureFlagHelper.isEnabled(accountId, FeatureName.EXPORT_TF_PLAN))
        .environmentVariables(terraformConfig.getEnvironmentVariables());

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(parameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformCommandUnit.Destroy.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    TerraformDestroyStepParameters parameters = (TerraformDestroyStepParameters) stepElementParameters.getSpec();
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    TerraformTaskNGResponse terraformTaskNGResponse = responseSupplier.get();
    List<UnitProgress> unitProgresses = terraformTaskNGResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : terraformTaskNGResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses);

    switch (terraformTaskNGResponse.getCommandExecutionStatus()) {
      case SUCCESS:
        stepResponseBuilder.status(Status.SUCCEEDED);
        break;
      case FAILURE:
        stepResponseBuilder.status(Status.FAILED);
        break;
      case RUNNING:
        stepResponseBuilder.status(Status.RUNNING);
        break;
      case QUEUED:
        stepResponseBuilder.status(Status.QUEUED);
        break;
      default:
        throw new InvalidRequestException(
            "Unhandled type CommandExecutionStatus: " + terraformTaskNGResponse.getCommandExecutionStatus().name(),
            WingsException.USER);
    }

    if (CommandExecutionStatus.SUCCESS == terraformTaskNGResponse.getCommandExecutionStatus()) {
      helper.clearTerraformConfig(parameters, ambiance);
    }
    return stepResponseBuilder.build();
  }
}