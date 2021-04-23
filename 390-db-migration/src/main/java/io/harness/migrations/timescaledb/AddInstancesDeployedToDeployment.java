package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
public class AddInstancesDeployedToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_instances_deployed_to_deployment.sql";
  }
}