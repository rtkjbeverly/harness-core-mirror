package io.harness.ng.cdOverview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class HealthDeploymentInfo {
  private TotalDeploymentInfo total;
  private DeploymentInfo success;
  private DeploymentInfo failure;
}