package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.execution.SdkResponseEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SuspendChainResponseEventHandler implements SdkResponseEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    SuspendChainRequest request = event.getSdkResponseEventRequest().getSuspendChainRequest();
    nodeExecutionService.update(request.getNodeExecutionId(),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    engine.resume(request.getNodeExecutionId(), request.getResponseMap(), request.getIsError());
  }
}