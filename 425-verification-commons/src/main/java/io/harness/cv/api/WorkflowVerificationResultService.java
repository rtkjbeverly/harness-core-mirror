package io.harness.cv.api;

import io.harness.beans.ExecutionStatus;
import io.harness.cv.WorkflowVerificationResult;

public interface WorkflowVerificationResultService {
  void addWorkflowVerificationResult(WorkflowVerificationResult workflowVerificationResult);

  void updateWorkflowVerificationResult(
      String stateExecutionId, boolean analyzed, ExecutionStatus executionStatus, String message);
}