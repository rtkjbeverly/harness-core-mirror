package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddRollbackToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_rollback_to_deployment.sql";
  }
}