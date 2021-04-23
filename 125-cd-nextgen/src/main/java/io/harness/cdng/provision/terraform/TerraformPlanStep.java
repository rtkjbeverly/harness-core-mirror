package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformCommand;
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
public class TerraformPlanStep extends TaskExecutableWithRollback<TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_PLAN.getYamlType()).build();

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
    TerraformPlanStepParameters planStepParameters = (TerraformPlanStepParameters) stepElementParameters.getSpec();
    TerraformTaskNGParametersBuilder builder = TerraformTaskNGParameters.builder();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    builder.accountId(accountId);
    String entityId = helper.generateFullIdentifier(planStepParameters.getProvisionerIdentifier(), ambiance);
    builder.taskType(TFTaskType.PLAN)
        .entityId(entityId)
        .currentStateFileId(helper.getLatestFileId(entityId))
        .workspace(ParameterFieldHelper.getParameterFieldValue(planStepParameters.getWorkspace()))
        .configFile(helper.getGitFetchFilesConfig(
            planStepParameters.getConfigFilesWrapper().getStoreConfig(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
        .inlineVarFiles(ParameterFieldHelper.getParameterFieldValue(planStepParameters.getInlineVarFiles()));
    if (EmptyPredicate.isNotEmpty(planStepParameters.getRemoteVarFiles())) {
      List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
      int i = 1;
      for (StoreConfigWrapper varFileWrapper : planStepParameters.getRemoteVarFiles()) {
        varFilesConfig.add(helper.getGitFetchFilesConfig(
            varFileWrapper.getStoreConfig(), ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
        i++;
      }
      builder.remoteVarfiles(varFilesConfig);
    }
    builder.backendConfig(ParameterFieldHelper.getParameterFieldValue(planStepParameters.getBackendConfig()))
        .targets(ParameterFieldHelper.getParameterFieldValue(planStepParameters.getTargets()))
        .saveTerraformStateJson(cdFeatureFlagHelper.isEnabled(accountId, FeatureName.EXPORT_TF_PLAN))
        .environmentVariables(helper.getEnvironmentVariablesMap(planStepParameters.getEnvironmentVariables()))
        .encryptionConfig(helper.getEncryptionConfig(ambiance, planStepParameters))
        .terraformCommand(TerraformPlanCommand.APPLY == planStepParameters.getTerraformPlanCommand()
                ? TerraformCommand.APPLY
                : TerraformCommand.DESTROY);

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.TERRAFORM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepElementParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(TerraformConstants.COMMAND_UNIT), TaskType.TERRAFORM_TASK_NG.getDisplayName());
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepElementParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseSupplier) throws Exception {
    TerraformPlanStepParameters planStepParameters = (TerraformPlanStepParameters) stepElementParameters.getSpec();
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
      helper.saveTerraformInheritOutput(planStepParameters, terraformTaskNGResponse, ambiance);
    }
    return stepResponseBuilder.build();
  }
}