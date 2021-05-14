package io.harness.cdng.artifact.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.steps.ServiceStepsHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome.ArtifactsOutcomeBuilder;
import io.harness.ngpipeline.artifact.bean.SidecarsOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepOutcomeGroup;
import io.harness.steps.common.NGForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class ArtifactsStep extends NGForkStep {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.ARTIFACTS.getName()).build();

  @Inject private ServiceStepsHelper serviceStepsHelper;

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, ForkStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    List<Outcome> outcomes = serviceStepsHelper.getChildrenOutcomes(responseDataMap);
    ArtifactsOutcomeBuilder builder = ArtifactsOutcome.builder();
    if (EmptyPredicate.isNotEmpty(outcomes)) {
      for (Outcome outcome : outcomes) {
        if (outcome instanceof ArtifactOutcome) {
          builder.primary((ArtifactOutcome) outcome);
        }
        if (outcome instanceof SidecarsOutcome) {
          builder.sidecars((SidecarsOutcome) outcome);
        }
      }
    }

    StepResponse stepResponse = super.handleChildrenResponse(ambiance, stepParameters, responseDataMap);
    return stepResponse.withStepOutcomes(Collections.singleton(StepResponse.StepOutcome.builder()
                                                                   .name(OutcomeExpressionConstants.ARTIFACTS)
                                                                   .outcome(builder.build())
                                                                   .group(StepOutcomeGroup.STAGE.name())
                                                                   .build()));
  }
}