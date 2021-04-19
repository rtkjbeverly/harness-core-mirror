package io.harness.batch.processing.tasklet;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.entities.ClusterDataDetails;
import io.harness.batch.processing.pricing.gcp.bigquery.impl.BigQueryHelperServiceImpl;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@OwnedBy(HarnessTeam.CE)
@Slf4j
public class DataCheckBigqueryAndTimescaleTasklet implements Tasklet {
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private BigQueryHelperServiceImpl bigQueryHelperService;
  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    ClusterDataDetails timeScaleClusterData =
        billingDataService.getTimeScaleClusterData(accountId, Instant.ofEpochMilli(startTime));
    ClusterDataDetails bigQueryClusterData =
        bigQueryHelperService.getClusterDataDetails(accountId, Instant.ofEpochMilli(startTime));
    if (timeScaleClusterData != null && bigQueryClusterData != null) {
      log.info("Timescale Billing data entries count {} , Timescale Sum of billing amount: {}",
          timeScaleClusterData.getEntriesCount(), timeScaleClusterData.getBillingAmountSum());
      log.info("Bigquery Billing data entries count {} , Bigquery Sum of billing amount: {}",
          bigQueryClusterData.getEntriesCount(), bigQueryClusterData.getBillingAmountSum());
      double timeScaleSum = timeScaleClusterData.getBillingAmountSum();
      double bigQuerySum = bigQueryClusterData.getBillingAmountSum();
      double differenceSum = Math.abs(timeScaleSum - bigQuerySum);
      if (timeScaleClusterData.getEntriesCount() == bigQueryClusterData.getEntriesCount() && differenceSum < 0.1) {
        log.info("Time Scale data matches with big query data");
      } else {
        log.error("TimeScale data doesn't  match with BigQuery data");
      }
    } else if (timeScaleClusterData == null) {
      log.info("Failed to retrieve TimeScale data");
      if (bigQueryClusterData == null) {
        log.info("Failed to retrieve BigQuery data");
      } else {
        log.info("Bigquery Billing data entries count {} , Bigquery Sum of billing amount: {}",
            bigQueryClusterData.getEntriesCount(), bigQueryClusterData.getBillingAmountSum());
      }
    } else {
      log.info("Timescale Billing data entries count {} , Timescale Sum of billing amount: {}",
          timeScaleClusterData.getEntriesCount(), timeScaleClusterData.getBillingAmountSum());
    }
    return null;
  }
}