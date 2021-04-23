package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.beans.baseline.WorkflowExecutionBaseline.WorkflowExecutionBaselineKeys;

public class AddAccountIdToWorkflowExecutionBaselines extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "workflowExecutionBaselines";
  }

  @Override
  protected String getFieldName() {
    return WorkflowExecutionBaselineKeys.accountId;
  }
}