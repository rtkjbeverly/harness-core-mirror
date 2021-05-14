package io.harness.cdng.service.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.VariablesSweepingOutput;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.cdng.variables.beans.NGVariableOverrideSets.NGVariableOverrideSetsSweepingOutput;
import io.harness.cdng.variables.beans.NGVariableOverrideSets.NGVariableOverrideSetsSweepingOutputInner;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.VisibleForTesting;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class ServiceSpecStep
    implements SyncExecutable<ServiceSpecStepParameters>, ChildrenExecutable<ServiceSpecStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.SERVICE_SPEC.getName()).build();

  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ServiceStepsHelper serviceStepsHelper;

  @Override
  public Class<ServiceSpecStepParameters> getStepParametersClass() {
    return ServiceSpecStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, ServiceSpecStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    NGLogCallback logCallback = serviceStepsHelper.getServiceLogCallback(ambiance);
    saveVariablesSweepingOutput(ambiance, stepParameters, logCallback);
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters, StepInputPackage inputPackage) {
    NGLogCallback logCallback = serviceStepsHelper.getServiceLogCallback(ambiance);
    saveVariablesSweepingOutput(ambiance, stepParameters, logCallback);
    return ChildrenExecutableResponse.newBuilder()
        .addAllChildren(stepParameters.getChildrenNodeIds()
                            .stream()
                            .map(id -> ChildrenExecutableResponse.Child.newBuilder().setChildNodeId(id).build())
                            .collect(Collectors.toList()))
        .build();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    StepResponse stepResponse = StepUtils.createStepResponseFromChildResponse(responseDataMap);
    if (StatusUtils.positiveStatuses().contains(stepResponse.getStatus())) {
      NGLogCallback logCallback = serviceStepsHelper.getServiceLogCallback(ambiance);
      logCallback.saveExecutionLog("Processed artifacts and manifests...");
    }
    return stepResponse;
  }

  private void saveVariablesSweepingOutput(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters, NGLogCallback logCallback) {
    saveExecutionLog(logCallback, "Processing service variables...");
    VariablesSweepingOutput variablesSweepingOutput = getVariablesSweepingOutput(ambiance, stepParameters, logCallback);
    executionSweepingOutputResolver.consume(ambiance, YAMLFieldNameConstants.VARIABLES, variablesSweepingOutput, null);

    Object outputObj = variablesSweepingOutput.get("output");
    if (!(outputObj instanceof VariablesSweepingOutput)) {
      outputObj = new VariablesSweepingOutput();
    }
    executionSweepingOutputResolver.consume(ambiance, YAMLFieldNameConstants.SERVICE_VARIABLES,
        (VariablesSweepingOutput) outputObj, StepOutcomeGroup.STAGE.name());

    NGVariableOverrideSetsSweepingOutput overrideSetsSweepingOutput =
        getVariablesOverrideSetsSweepingOutput(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(ambiance, "variableOverrideSets", overrideSetsSweepingOutput, null);
    saveExecutionLog(logCallback, "Processed service variables");
  }

  private VariablesSweepingOutput getVariablesSweepingOutput(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters, NGLogCallback logCallback) {
    Map<String, Object> variables = getFinalVariablesMap(ambiance, stepParameters, logCallback);
    VariablesSweepingOutput variablesOutcome = new VariablesSweepingOutput();
    variablesOutcome.putAll(variables);
    return variablesOutcome;
  }

  @VisibleForTesting
  Map<String, Object> getFinalVariablesMap(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters, NGLogCallback logCallback) {
    List<NGVariable> variableList = stepParameters.getOriginalVariables();
    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> outputVariables = new VariablesSweepingOutput();
    if (EmptyPredicate.isNotEmpty(variableList)) {
      Map<String, Object> originalVariables =
          NGVariablesUtils.getMapOfVariables(variableList, ambiance.getExpressionFunctorToken());
      variables.putAll(originalVariables);
      outputVariables.putAll(originalVariables);
    }
    outputVariables = addOverrideSets(ambiance, outputVariables, stepParameters, logCallback);
    outputVariables = addStageOverrides(ambiance, outputVariables, stepParameters, logCallback);
    variables.put("output", outputVariables);
    return variables;
  }

  private Map<String, Object> addOverrideSets(Ambiance ambiance, Map<String, Object> variables,
      ServiceSpecStepParameters stepParameters, NGLogCallback logCallback) {
    if (ParameterField.isNull(stepParameters.getStageOverridesUseVariableOverrideSets())) {
      return variables;
    }

    for (String useVariableOverrideSet : stepParameters.getStageOverridesUseVariableOverrideSets().getValue()) {
      List<NGVariableOverrideSets> variableOverrideSetsList =
          stepParameters.getOriginalVariableOverrideSets()
              .stream()
              .map(NGVariableOverrideSetWrapper::getOverrideSet)
              .filter(overrideSet -> overrideSet.getIdentifier().equals(useVariableOverrideSet))
              .collect(Collectors.toList());
      if (variableOverrideSetsList.size() == 0) {
        throw new InvalidRequestException(
            String.format("Invalid identifier [%s] in variable override sets", useVariableOverrideSet));
      }
      if (variableOverrideSetsList.size() > 1) {
        throw new InvalidRequestException(
            String.format("Duplicate identifier [%s] in variable override sets", useVariableOverrideSet));
      }

      saveExecutionLog(
          logCallback, String.format("Applying service variable overrides set: %s", useVariableOverrideSet));
      variables = NGVariablesUtils.applyVariableOverrides(
          variables, variableOverrideSetsList.get(0).getVariables(), ambiance.getExpressionFunctorToken());
    }
    return variables;
  }

  private Map<String, Object> addStageOverrides(Ambiance ambiance, Map<String, Object> variables,
      ServiceSpecStepParameters stepParameters, NGLogCallback logCallback) {
    if (EmptyPredicate.isEmpty(stepParameters.getStageOverrideVariables())) {
      return variables;
    }

    saveExecutionLog(logCallback, "Applying service variable stage overrides");
    return NGVariablesUtils.applyVariableOverrides(
        variables, stepParameters.getStageOverrideVariables(), ambiance.getExpressionFunctorToken());
  }

  private NGVariableOverrideSetsSweepingOutput getVariablesOverrideSetsSweepingOutput(
      Ambiance ambiance, ServiceSpecStepParameters stepParameters) {
    NGVariableOverrideSetsSweepingOutput overrideSetsSweepingOutput = new NGVariableOverrideSetsSweepingOutput();
    if (stepParameters.getOriginalVariableOverrideSets() == null) {
      return overrideSetsSweepingOutput;
    }
    stepParameters.getOriginalVariableOverrideSets().forEach(overrideSets
        -> overrideSetsSweepingOutput.put(overrideSets.getOverrideSet().getIdentifier(),
            new NGVariableOverrideSetsSweepingOutputInner(NGVariablesUtils.getMapOfVariables(
                overrideSets.getOverrideSet().getVariables(), ambiance.getExpressionFunctorToken()))));
    return overrideSetsSweepingOutput;
  }

  private void saveExecutionLog(NGLogCallback logCallback, String line) {
    if (logCallback != null) {
      logCallback.saveExecutionLog(line);
    }
  }
}