package io.harness.audit.beans.custom.user;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.beans.custom.AuditEventDataTypeConstants.USER_INVITE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(USER_INVITE)
@TypeAlias("UserInviteAuditEventData")
public class UserInviteAuditEventData extends AuditEventData {
  List<RoleBinding> roleBindings;

  @Builder
  public UserInviteAuditEventData(List<RoleBinding> roleBindings) {
    this.roleBindings = roleBindings;
    this.type = USER_INVITE;
  }
}