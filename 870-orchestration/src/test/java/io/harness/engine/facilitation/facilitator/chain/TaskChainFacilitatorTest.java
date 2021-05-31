package io.harness.engine.facilitation.facilitator.chain;

import static io.harness.pms.contracts.execution.ExecutionMode.TASK_CHAIN;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.execution.facilitator.DefaultFacilitatorParams;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TaskChainFacilitatorTest extends OrchestrationTestBase {
  @Inject private TaskChainFacilitator taskChainFacilitator;
  @Inject private KryoSerializer kryoSerializer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFacilitate() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    byte[] parameters = kryoSerializer.asBytes(DefaultFacilitatorParams.builder().build());
    FacilitatorResponseProto response = taskChainFacilitator.facilitate(ambiance, parameters);
    assertThat(response).isNotNull();
    assertThat(response.getExecutionMode()).isEqualTo(TASK_CHAIN);
    assertThat(response.getInitialWait().getSeconds()).isEqualTo(0);
  }
}