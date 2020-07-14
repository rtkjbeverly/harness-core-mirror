package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoreConfigWrapper implements OverridesApplier<StoreConfigWrapper> {
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  @Wither
  StoreConfig storeConfig;

  @Builder
  public StoreConfigWrapper(String type, StoreConfig storeConfig) {
    this.type = type;
    this.storeConfig = storeConfig;
  }

  @Override
  public StoreConfigWrapper applyOverrides(StoreConfigWrapper overrideConfig) {
    StoreConfigWrapper resultantConfig = this;
    if (overrideConfig != null) {
      resultantConfig = resultantConfig.withStoreConfig(storeConfig.applyOverrides(overrideConfig.getStoreConfig()));
    }
    return resultantConfig;
  }
}
