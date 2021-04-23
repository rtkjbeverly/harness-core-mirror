package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;

import software.wings.service.impl.analysis.TimeSeriesRiskSummary.TimeSeriesRiskSummaryKeys;

public class AddAccountIdToTimeSeriesRiskSummary extends AddAccountIdToCollectionUsingAppIdMigration {
  @Override
  protected String getCollectionName() {
    return "timeSeriesRiskSummary";
  }

  @Override
  protected String getFieldName() {
    return TimeSeriesRiskSummaryKeys.accountId;
  }
}