package io.harness.steps.approval.step.beans;

import io.harness.steps.approval.step.entities.ApprovalInstance;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ApprovalInstanceResponse")
public class ApprovalInstanceResponseDTO {
  @NotEmpty String id;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  @NotNull String approvalMessage;
  boolean includePipelineExecutionHistory;
  long deadline;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  ApprovalInstanceDetailsDTO details;

  Long createdAt;
  Long lastModifiedAt;

  public static ApprovalInstanceResponseDTO fromApprovalInstance(ApprovalInstance instance) {
    if (instance == null) {
      return null;
    }
    return instance.toApprovalInstanceResponseDTO();
  }
}