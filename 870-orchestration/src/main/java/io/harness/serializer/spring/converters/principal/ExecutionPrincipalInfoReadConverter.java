package io.harness.serializer.spring.converters.principal;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.serializer.spring.ProtoReadConverter;

@OwnedBy(PIPELINE)
public class ExecutionPrincipalInfoReadConverter extends ProtoReadConverter<ExecutionPrincipalInfo> {
  public ExecutionPrincipalInfoReadConverter() {
    super(ExecutionPrincipalInfo.class);
  }
}