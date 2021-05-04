package io.harness.audit.beans.custom.user;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.beans.custom.AuditEventDataTypeConstants.ADD_COLLABORATOR_AUDIT_EVENT_DATA;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(ADD_COLLABORATOR_AUDIT_EVENT_DATA)
@TypeAlias(ADD_COLLABORATOR_AUDIT_EVENT_DATA)
public class AddCollaboratorAuditEventData extends AuditEventData {
  @NotNull @Valid Source source;

  @Builder
  public AddCollaboratorAuditEventData(Source source) {
    this.source = source;
    this.type = ADD_COLLABORATOR_AUDIT_EVENT_DATA;
  }
}