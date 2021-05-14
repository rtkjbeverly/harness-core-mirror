package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.rollback.NGFailureActionTypeConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public enum NGFailureActionType {
  @JsonProperty(NGFailureActionTypeConstants.IGNORE) IGNORE(NGFailureActionTypeConstants.IGNORE),
  @JsonProperty(NGFailureActionTypeConstants.RETRY) RETRY(NGFailureActionTypeConstants.RETRY),
  @JsonProperty(NGFailureActionTypeConstants.MARK_AS_SUCCESS)
  MARK_AS_SUCCESS(NGFailureActionTypeConstants.MARK_AS_SUCCESS),
  @JsonProperty(NGFailureActionTypeConstants.ABORT) ABORT(NGFailureActionTypeConstants.ABORT),
  @JsonProperty(NGFailureActionTypeConstants.STAGE_ROLLBACK)
  STAGE_ROLLBACK(NGFailureActionTypeConstants.STAGE_ROLLBACK),
  @JsonProperty(NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK)
  STEP_GROUP_ROLLBACK(NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK),
  @JsonProperty(NGFailureActionTypeConstants.MANUAL_INTERVENTION)
  MANUAL_INTERVENTION(NGFailureActionTypeConstants.MANUAL_INTERVENTION);

  String yamlName;

  NGFailureActionType(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonCreator
  public static NGFailureActionType getFailureActionType(@JsonProperty("action") String yamlName) {
    for (NGFailureActionType value : NGFailureActionType.values()) {
      if (value.yamlName.equalsIgnoreCase(yamlName)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }
}