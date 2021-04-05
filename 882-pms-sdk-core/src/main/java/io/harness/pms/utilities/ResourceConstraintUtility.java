package io.harness.pms.utilities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.utils.PmsConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class ResourceConstraintUtility {
  public JsonNode getResourceConstraintJsonNode(String resourceUnit) {
    String yamlField = "---\n"
        + "name: \"Resource Constraint\"\n"
        + "timeout: \"1w\"\n"
        + "type: \"ResourceConstraint\"\n"
        + "spec:\n"
        + "  name: \"" + PmsConstants.QUEUING_RC_NAME + "\"\n"
        + "  resourceUnit: \"" + resourceUnit + "\"\n"
        + "  acquireMode: \"ENSURE\"\n"
        + "  permits: 1\n"
        + "  holdingScope:\n"
        + "    scope: \"PLAN\"\n";
    YamlField resourceConstraintYamlField;
    try {
      String yamlFieldWithUuid = YamlUtils.injectUuid(yamlField);
      resourceConstraintYamlField = YamlUtils.readTree(yamlFieldWithUuid);
    } catch (IOException e) {
      throw new InvalidRequestException("Exception while creating resource constraint");
    }
    return resourceConstraintYamlField.getNode().getCurrJsonNode();
  }
}