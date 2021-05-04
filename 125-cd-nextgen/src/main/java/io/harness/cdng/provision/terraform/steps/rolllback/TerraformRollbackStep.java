package io.harness.cdng.provision.terraform.steps.rolllback;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.cdng.provision.terraform.TerraformConfig;
import io.harness.cdng.provision.terraform.TerraformConfig.TerraformConfigKeys;
import io.harness.cdng.provision.terraform.TerraformStepHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformCommandUnit;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters.TerraformTaskNGParametersBuilder;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.provision.TerraformConstants;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mongodb.morphia.query.Sort;

public class TerraformRollbackStep extends TaskExecutableWithRollback<TerraformTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TERRAFORM_ROLLBACK.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private HPersistence persistence;
  @Inject private TerraformStepHelper terraformStepHelper;
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    TerraformRollbackStepParameters stepParametersSpec = (TerraformRollbackStepParameters) stepParameters.getSpec();

    String provisionerIdentifier = stepParametersSpec.getProvisionerIdentifier();
    String entityId =
        terraformStepHelper.generateFullIdentifier(stepParametersSpec.getProvisionerIdentifier(), ambiance);
    try (HIterator<TerraformConfig> configIterator =
             new HIterator(persistence.createQuery(TerraformConfig.class)
                               .filter(TerraformConfigKeys.accountId, AmbianceHelper.getAccountId(ambiance))
                               .filter(TerraformConfigKeys.orgId, AmbianceHelper.getOrgIdentifier(ambiance))
                               .filter(TerraformConfigKeys.projectId, AmbianceHelper.getProjectIdentifier(ambiance))
                               .filter(TerraformConfigKeys.entityId, entityId)
                               .order(Sort.descending(TerraformConfigKeys.createdAt))
                               .fetch())) {
      if (!configIterator.hasNext()) {
        return TaskRequest.newBuilder()
            .setSkipTaskRequest(
                SkipTaskRequest.newBuilder()
                    .setMessage(
                        format("No successful Provisioning found with provisionerIdentifier: [%s]. Skipping rollback.",
                            provisionerIdentifier))
                    .build())
            .build();
      }

      TerraformConfig rollbackConfig = null;
      TerraformConfig currentConfig = null;
      while (configIterator.hasNext()) {
        rollbackConfig = configIterator.next();

        if (rollbackConfig.getPipelineExecutionId().equals(ambiance.getPlanExecutionId())) {
          if (currentConfig == null) {
            currentConfig = rollbackConfig;
          }
        } else {
          // Found previous successful terraform config
          break;
        }
      }
      StringBuilder rollbackMessage = new StringBuilder();
      TFTaskType tfTaskType;
      if (rollbackConfig == currentConfig) {
        rollbackMessage.append(
            format("No previous successful Terraform execution exists with the identifier : [%s], hence Destroying.",
                provisionerIdentifier));
        tfTaskType = TFTaskType.DESTROY;
      } else {
        tfTaskType = TFTaskType.APPLY;
        rollbackMessage.append("Inheriting Terraform Config from last successful Terraform Execution : ");
        rollbackMessage.append(prepareExecutionUrl(rollbackConfig.getPipelineExecutionId(), ambiance));
      }
      // TODO:  log rollback message
      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.TERRAFORM_CONFIG,
          TerraformConfigSweepingOutput.builder().terraformConfig(rollbackConfig).tfTaskType(tfTaskType).build(),
          StepOutcomeGroup.STEP.name());

      TerraformTaskNGParametersBuilder builder =
          TerraformTaskNGParameters.builder()
              .accountId(AmbianceHelper.getAccountId(ambiance))
              .currentStateFileId(terraformStepHelper.getLatestFileId(entityId))
              .taskType(tfTaskType)
              .terraformCommandUnit(TerraformCommandUnit.Rollback)
              .entityId(entityId)
              .workspace(rollbackConfig.getWorkspace())
              .configFile(terraformStepHelper.getGitFetchFilesConfig(
                  rollbackConfig.getConfigFiles().toGitStoreConfig(), ambiance, TerraformStepHelper.TF_CONFIG_FILES))
              .inlineVarFiles(rollbackConfig.getInlineVarFiles());
      if (isNotEmpty(rollbackConfig.getRemoteVarFiles())) {
        List<GitFetchFilesConfig> varFilesConfig = new ArrayList<>();
        int i = 1;
        for (GitStoreConfigDTO remoteVarFileConfig : rollbackConfig.getRemoteVarFiles()) {
          varFilesConfig.add(terraformStepHelper.getGitFetchFilesConfig(
              remoteVarFileConfig.toGitStoreConfig(), ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i)));
          i++;
        }
        builder.remoteVarfiles(varFilesConfig);
      }
      builder.backendConfig(rollbackConfig.getBackendConfig())
          .targets(rollbackConfig.getTargets())
          .environmentVariables(rollbackConfig.getEnvironmentVariables());

      TaskData taskData =
          TaskData.builder()
              .async(true)
              .taskType(TaskType.TERRAFORM_TASK_NG.name())
              .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
              .parameters(new Object[] {builder.build()})
              .build();

      ParameterField<List<String>> delegateSelectors = stepParameters.getDelegateSelectors();

      List<TaskSelector> taskSelectors = StepUtils.getTaskSelectors(delegateSelectors);

      return StepUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
          Collections.singletonList(TerraformCommandUnit.Rollback.name()), TaskType.TERRAFORM_TASK_NG.getDisplayName(),
          taskSelectors);
    }
  }

  private String prepareExecutionUrl(String pipelineExecutionId, Ambiance ambiance) {
    // TODO: prepare execution url
    return null;
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<TerraformTaskNGResponse> responseDataSupplier) throws Exception {
    TerraformRollbackStepParameters stepParametersSpec = (TerraformRollbackStepParameters) stepParameters.getSpec();
    TerraformTaskNGResponse taskResponse = responseDataSupplier.get();
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    List<UnitProgress> unitProgresses = taskResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : taskResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses)
        .status(StepUtils.getStepStatus(taskResponse.getCommandExecutionStatus()));

    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.TERRAFORM_CONFIG));
    TerraformConfigSweepingOutput rollbackConfigOutput =
        (TerraformConfigSweepingOutput) optionalSweepingOutput.getOutput();
    TerraformConfig rollbackConfig = rollbackConfigOutput.getTerraformConfig();

    if (taskResponse.getStateFileId() != null) {
      terraformStepHelper.updateParentEntityIdAndVersion(rollbackConfig.getEntityId(), taskResponse.getStateFileId());
    }

    if (taskResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      if (rollbackConfigOutput.getTfTaskType() == TFTaskType.APPLY) {
        terraformStepHelper.saveTerraformConfig(rollbackConfig, ambiance);
      } else {
        terraformStepHelper.clearTerraformConfig(ambiance, rollbackConfig.getEntityId());
      }
    }
    return stepResponseBuilder.build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}