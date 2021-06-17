package io.harness.pms.helpers;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.execution.NodeExecution;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class YamlExpressionResolveHelper {
  @Inject private PmsEngineExpressionService pmsEngineExpressionService;
  @Inject private NodeExecutionService nodeExecutionService;

  public String resolveExpressionsInYaml(String yamlString, String planExecutionId) {
    Optional<NodeExecution> nodeExecution = nodeExecutionService.getByNodeIdentifier("pipeline", planExecutionId);

    if (nodeExecution.isPresent()) {
      EngineExpressionEvaluator engineExpressionEvaluator =
          pmsEngineExpressionService.prepareExpressionEvaluator(nodeExecution.get().getAmbiance());
      try {
        YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlString));
        YamlField pipelineYamlField = yamlField.getNode().getField("pipeline");

        resolveExpressions(Preconditions.checkNotNull(pipelineYamlField, "YAML does not have pipeline object"),
            engineExpressionEvaluator);

        JsonNode resolvedYamlNode = yamlField.getNode().getCurrJsonNode();
        YamlUtils.removeUuid(resolvedYamlNode);
        return YamlPipelineUtils.writeString(resolvedYamlNode).replace("---\n", "").replace("\"", "");

      } catch (IOException ex) {
        log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
        throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      }
    }

    throw new InvalidRequestException("Invalid request : No execution details found");
  }

  private void resolveExpressions(YamlField field, EngineExpressionEvaluator engineExpressionEvaluator) {
    if (field.getNode().isObject()) {
      resolveExpressionsInObject(field, engineExpressionEvaluator);
    } else if (field.getNode().isArray()) {
      resolveExpressionsInArray(field.getNode(), engineExpressionEvaluator);
    }
  }

  private void resolveExpressionsInObject(YamlField field, EngineExpressionEvaluator engineExpressionEvaluator) {
    for (YamlField childYamlField : field.getNode().fields()) {
      if (childYamlField.getNode().getCurrJsonNode().isValueNode()) {
        resolveExpressionInValueNode(field, childYamlField.getName(),
            childYamlField.getNode().getCurrJsonNode().asText(), engineExpressionEvaluator);
      } else if (YamlUtils.checkIfNodeIsArrayWithPrimitiveTypes(field.getNode().getCurrJsonNode())) {
        continue;
      } else {
        resolveExpressions(childYamlField, engineExpressionEvaluator);
      }
    }
  }

  private void resolveExpressionsInArray(YamlNode yamlNode, EngineExpressionEvaluator engineExpressionEvaluator) {
    for (YamlNode arrayElement : yamlNode.asArray()) {
      if (arrayElement.isObject()) {
        for (YamlField field : arrayElement.fields()) {
          resolveExpressions(field, engineExpressionEvaluator);
        }
      } else if (arrayElement.isArray()) {
        resolveExpressionsInArray(arrayElement, engineExpressionEvaluator);
      }
    }
  }

  private void resolveExpressionInValueNode(
      YamlField parentField, String childName, String childValue, EngineExpressionEvaluator engineExpressionEvaluator) {
    ObjectNode objectNode = (ObjectNode) parentField.getNode().getCurrJsonNode();
    if (EngineExpressionEvaluator.hasVariables(childValue)) {
      String expression = "<+" + YamlUtils.getFullyQualifiedName(parentField.getNode()) + "." + childName + ">";
      String resolvedExpression = engineExpressionEvaluator.renderExpression(expression, true);
      // Update node value only if expression was successfully resolved
      if (!resolvedExpression.equals(expression)) {
        objectNode.put(childName, resolvedExpression);
      }
    }
  }
}