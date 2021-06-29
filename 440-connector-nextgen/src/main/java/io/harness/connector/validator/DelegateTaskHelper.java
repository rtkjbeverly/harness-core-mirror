package io.harness.connector.validator;

import static io.harness.utils.DelegateOwner.getNGTaskSetupAbstractionsWithOwner;

import io.harness.beans.DelegateTaskRequest;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import lombok.experimental.UtilityClass;

@Singleton
@UtilityClass
public class DelegateTaskHelper {
  public DelegateTaskRequest buildDelegateTask(TaskParameters taskParameters, ConnectorConfigDTO connectorConfig,
      String taskType, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (taskParameters instanceof ConnectorTaskParams && connectorConfig instanceof DelegateSelectable) {
      ((ConnectorTaskParams) taskParameters)
          .setDelegateSelectors(((DelegateSelectable) connectorConfig).getDelegateSelectors());
    }

    final Map<String, String> ngTaskSetupAbstractionsWithOwner =
        getNGTaskSetupAbstractionsWithOwner(accountIdentifier, orgIdentifier, projectIdentifier);

    return DelegateTaskRequest.builder()
        .accountId(accountIdentifier)
        .taskType(taskType)
        .taskParameters(taskParameters)
        .taskSetupAbstractions(ngTaskSetupAbstractionsWithOwner)
        .executionTimeout(Duration.ofMinutes(2))
        .forceExecute(true)
        .build();
  }
}