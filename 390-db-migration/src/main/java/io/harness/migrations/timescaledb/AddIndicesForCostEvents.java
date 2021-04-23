package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;

public class AddIndicesForCostEvents extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_indices_cost_events.sql";
  }
}