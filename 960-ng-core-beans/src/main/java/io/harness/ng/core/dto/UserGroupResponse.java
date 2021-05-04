package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
public class UserGroupResponse {
  @NotNull private UserGroupDTO userGroup;
  private Long createdAt;
  private Long lastModifiedAt;

  @Builder
  public UserGroupResponse(UserGroupDTO userGroup, Long createdAt, Long lastModifiedAt) {
    this.userGroup = userGroup;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}