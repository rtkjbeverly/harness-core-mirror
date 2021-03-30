package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("Remote")
public class RemoteTerraformVarFileSpec implements TerraformVarFileSpec {
  @JsonProperty("store") StoreConfigWrapper storeConfigWrapper;
}