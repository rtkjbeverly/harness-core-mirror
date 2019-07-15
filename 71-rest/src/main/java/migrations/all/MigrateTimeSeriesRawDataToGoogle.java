package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.persistence.HIterator;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.math3.util.Pair;
import org.mongodb.morphia.query.Query;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData.ContinuousVerificationExecutionMetaDataKeys;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesRawData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MigrateTimeSeriesRawDataToGoogle implements Migration {
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;

  @Override
  public void migrate() {
    if (dataStoreService instanceof MongoDataStoreServiceImpl) {
      logger.info("DataStore service is an instance of MongoDB. Not migrating the records now");
      return;
    }

    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(180);
    List<TimeSeriesRawData> rawData = new ArrayList<>();

    try {
      Query<ContinuousVerificationExecutionMetaData> cvExecutionQuery =
          wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
              .field(ContinuousVerificationExecutionMetaDataKeys.workflowStartTs)
              .greaterThanOrEq(startTime);

      // Map of (account id, Map of (state execution id, Pair (execution status, service id)))
      Map<String, Map<String, Pair<ExecutionStatus, String>>> cvExecutionRecordMap = new HashMap<>();
      try (HIterator<ContinuousVerificationExecutionMetaData> records = new HIterator<>(cvExecutionQuery.fetch())) {
        while (records.hasNext()) {
          ContinuousVerificationExecutionMetaData record = records.next();
          if (VerificationConstants.getMetricAnalysisStates().contains(record.getStateType())) {
            if (!cvExecutionRecordMap.containsKey(record.getAccountId())) {
              cvExecutionRecordMap.put(record.getAccountId(), new HashMap<>());
            }
            cvExecutionRecordMap.get(record.getAccountId())
                .put(record.getStateExecutionId(), new Pair<>(record.getExecutionStatus(), record.getServiceId()));
          }
        }
      }

      for (Map.Entry<String, Map<String, Pair<ExecutionStatus, String>>> executionRecords :
          cvExecutionRecordMap.entrySet()) {
        String accountId = executionRecords.getKey();
        Query<TimeSeriesMLAnalysisRecord> query =
            wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
                .field("stateExecutionId")
                .in(executionRecords.getValue().keySet())
                .order("stateExecutionId");

        Map<String, Map<String, TimeSeriesRawData>> rawDataMap = new HashMap<>();
        String previousStateExecutionId = null;

        try (HIterator<TimeSeriesMLAnalysisRecord> records = new HIterator<>(query.fetch())) {
          while (records.hasNext()) {
            TimeSeriesMLAnalysisRecord record = records.next();

            if (previousStateExecutionId == null) {
              previousStateExecutionId = record.getStateExecutionId();
            }

            if (!previousStateExecutionId.equals(record.getStateExecutionId())) {
              rawDataMap.values().forEach(metricMap -> rawData.addAll(metricMap.values()));
              rawDataMap = new HashMap<>();
              previousStateExecutionId = record.getStateExecutionId();
            }

            Pair<ExecutionStatus, String> cvMetaData = executionRecords.getValue().get(record.getStateExecutionId());
            TimeSeriesRawData.populateRawDataFromAnalysisRecords(
                record, accountId, cvMetaData.getKey(), rawDataMap, cvMetaData.getValue());
            // rawData.addAll(rawDataForCurrentRecord);
            if (rawData.size() >= 1000) {
              dataStoreService.save(TimeSeriesRawData.class, rawData, true);
              logger.info("Copied {} raw data records from Mongo to GoogleDataStore", rawData.size());
              rawData.clear();
              sleep(ofMillis(1500));
            }
          }
        }
      }
      dataStoreService.save(TimeSeriesRawData.class, rawData, true);
      logger.info("Copied {} raw data records from Mongo to GoogleDataStore", rawData.size());
    } catch (Exception e) {
      logger.error("Exception occurred while migrate time series raw data to GDS", e);
    }
  }
}
