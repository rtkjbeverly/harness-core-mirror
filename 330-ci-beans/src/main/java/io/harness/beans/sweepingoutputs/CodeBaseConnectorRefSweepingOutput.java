package io.harness.beans.sweepingoutputs;

import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODE_BASE_CONNECTOR_REF;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
@TypeAlias(CODE_BASE_CONNECTOR_REF)
@JsonTypeName(CODE_BASE_CONNECTOR_REF)
public class CodeBaseConnectorRefSweepingOutput implements ExecutionSweepingOutput {
  String codeBaseConnectorRef;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore String uuid;
}