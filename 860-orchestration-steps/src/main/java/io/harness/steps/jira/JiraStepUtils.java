package io.harness.steps.jira;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.jira.beans.JiraField;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class JiraStepUtils {
  public Map<String, ParameterField<String>> prepareFieldMap(List<JiraField> fields) {
    if (fields == null) {
      return null;
    }

    Map<String, ParameterField<String>> fieldsMap = new HashMap<>();
    Set<String> duplicateFields = new HashSet<>();
    fields.forEach(field -> {
      if (fieldsMap.containsKey(field.getName())) {
        duplicateFields.add(field.getName());
      } else {
        fieldsMap.put(field.getName(), field.getValue());
      }
    });

    if (EmptyPredicate.isNotEmpty(duplicateFields)) {
      throw new InvalidRequestException(
          String.format("Duplicate jira fields: [%s]", String.join(", ", duplicateFields)));
    }
    return fieldsMap;
  }
}