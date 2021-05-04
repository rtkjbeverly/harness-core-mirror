package io.harness.cdng.provision.terraform.steps.rolllback;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.terraform.TerraformConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("TerraformConfigSweepingOutput")
@JsonTypeName("TerraformConfigSweepingOutput")
public class TerraformConfigSweepingOutput implements ExecutionSweepingOutput {
  TerraformConfig terraformConfig;
  TFTaskType tfTaskType;

  @Override
  public String getType() {
    return "TerraformConfigSweepingOutput";
  }
}