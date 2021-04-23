package io.harness.engine.facilitation;

import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.expression.EngineExpressionService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SkipPreFacilitationChecker extends ExpressionEvalPreFacilitationChecker {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  protected PreFacilitationCheck performCheck(NodeExecution nodeExecution) {
    log.info("Checking If Node should be Skipped");
    Ambiance ambiance = nodeExecution.getAmbiance();
    String skipCondition = nodeExecution.getNode().getSkipCondition();
    if (EmptyPredicate.isNotEmpty(skipCondition)) {
      try {
        boolean skipConditionValue = (Boolean) engineExpressionService.evaluateExpression(ambiance, skipCondition);
        nodeExecutionService.update(nodeExecution.getUuid(), ops -> {
          ops.set(NodeExecutionKeys.skipInfo,
              SkipInfo.newBuilder().setEvaluatedCondition(skipConditionValue).setSkipCondition(skipCondition).build());
        });
        if (skipConditionValue) {
          log.info(String.format("Skipping node: %s", nodeExecution.getUuid()));
          StepResponseProto response =
              StepResponseProto.newBuilder()
                  .setStatus(Status.SKIPPED)
                  .setSkipInfo(
                      SkipInfo.newBuilder().setSkipCondition(skipCondition).setEvaluatedCondition(true).build())
                  .build();
          orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), response);
          return PreFacilitationCheck.builder().proceed(false).reason("Skip Condition Evaluated to true").build();
        }
        return PreFacilitationCheck.builder().proceed(true).reason("Skip Condition Evaluated to false").build();
      } catch (Exception ex) {
        return handleExpressionEvaluationError(nodeExecution.getUuid(), ex);
      }
    }
    return PreFacilitationCheck.builder().proceed(true).reason("No Skip Condition Configured").build();
  }
}