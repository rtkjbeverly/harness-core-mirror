package io.harness.delegate.beans.connector.gcpccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GcpBillingExportSpec")
@OwnedBy(CE)
public class GcpBillingExportSpecDTO {
  @NotNull String projectId;
  @NotNull String datasetId;

  @Builder
  public GcpBillingExportSpecDTO(String projectId, String datasetId) {
    this.projectId = projectId;
    this.datasetId = datasetId;
  }
}