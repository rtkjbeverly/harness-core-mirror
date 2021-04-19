package io.harness.batch.processing.cleanup;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.ccm.commons.dao.CEDataCleanupRequestDao;
import io.harness.ccm.commons.entities.CEDataCleanupRequest;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class CEDataCleanupRequestService {
  @Autowired private CEDataCleanupRequestDao ceDataCleanupRequestDao;
  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;
  @Autowired private BillingDataServiceImpl billingDataService;

  public void processDataCleanUpRequest() {
    List<CEDataCleanupRequest> dataCleanupRequests = ceDataCleanupRequestDao.getNotProcessedDataCleanupRequests();
    for (CEDataCleanupRequest dataCleanupRequest : dataCleanupRequests) {
      String jobType = dataCleanupRequest.getBatchJobType();
      try {
        BatchJobType batchJobType = BatchJobType.valueOf(jobType);
        log.info("Processing data cleanup request {}", dataCleanupRequest.getUuid());
        batchJobScheduledDataService.invalidateJobs(dataCleanupRequest);
        if (ImmutableSet.of(BatchJobType.INSTANCE_BILLING, BatchJobType.INSTANCE_BILLING_HOURLY)
                .contains(batchJobType)) {
          billingDataService.cleanBillingData(
              dataCleanupRequest.getAccountId(), dataCleanupRequest.getStartAt(), Instant.now(), batchJobType);
        }
      } catch (Exception ex) {
        log.error("Exception while processing request", ex);
      } finally {
        ceDataCleanupRequestDao.updateRequestStatus(dataCleanupRequest);
      }
    }
  }
}