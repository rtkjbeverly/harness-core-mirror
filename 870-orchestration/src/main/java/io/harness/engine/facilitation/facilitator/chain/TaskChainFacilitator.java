package io.harness.engine.facilitation.facilitator.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.facilitation.facilitator.CoreFacilitator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.facilitator.FacilitatorUtils;
import io.harness.serializer.ProtoUtils;

import com.google.inject.Inject;
import java.time.Duration;

@OwnedBy(CDC)
public class TaskChainFacilitator implements CoreFacilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK_CHAIN).build();

  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponseProto facilitate(Ambiance ambiance, byte[] parameters) {
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    return FacilitatorResponseProto.newBuilder()
        .setExecutionMode(ExecutionMode.TASK_CHAIN)
        .setInitialWait(ProtoUtils.javaDurationToDuration(waitDuration))
        .setIsSuccessful(true)
        .build();
  }
}