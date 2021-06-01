package io.harness.accesscontrol.aggregator;

import io.harness.aggregator.AggregatorMetricsService;
import io.harness.aggregator.SnapshotMetrics;
import io.harness.aggregator.StreamingMetrics;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class AggregatorStackDriverMetricsPublisherImpl implements MetricsPublisher {
  private final AggregatorMetricsService aggregatorMetricsService;
  private final MetricService metricService;

  @Override
  public void recordMetrics() {
    Optional<StreamingMetrics> streamingMetricsOptional = aggregatorMetricsService.getStreamingMetrics();
    Optional<SnapshotMetrics> snapshotMetricsOptional = aggregatorMetricsService.getSnapshotMetrics();

    if (streamingMetricsOptional.isPresent()) {
      StreamingMetrics streamingMetrics = streamingMetricsOptional.get();
      metricService.recordMetric("aggregator_streaming_numberOfDisconnects", streamingMetrics.getNumberOfDisconnects());
      metricService.recordMetric(
          "aggregator_streaming_numberOfPrimaryElections", streamingMetrics.getNumberOfPrimaryElections());
      metricService.recordMetric("aggregator_streaming_millisBehindSource", streamingMetrics.getMillisBehindSource());
      metricService.recordMetric("aggregator_streaming_connected", streamingMetrics.isConnected() ? 1 : 0);
      metricService.recordMetric(
          "aggregator_streaming_millisSinceLastEvent", streamingMetrics.getMillisSinceLastEvent());
      metricService.recordMetric("aggregator_streaming_queueTotalCapacity", streamingMetrics.getQueueTotalCapacity());
      metricService.recordMetric(
          "aggregator_streaming_queueRemainingCapacity", streamingMetrics.getQueueRemainingCapacity());
      log.info("Streaming=> Last event seen: {}", streamingMetrics.getLastEvent());
      metricService.recordMetric(
          "aggregator_streaming_totalNumberOfEventsSeen", streamingMetrics.getTotalNumberOfEventsSeen());
      metricService.recordMetric(
          "aggregator_streaming_currentQueueSizeInBytes", streamingMetrics.getCurrentQueueSizeInBytes());
    } else {
      log.info("Streaming metrics are not yet available.");
    }

    if (snapshotMetricsOptional.isPresent()) {
      SnapshotMetrics snapshotMetrics = snapshotMetricsOptional.get();
      metricService.recordMetric("aggregator_snapshot_millisSinceLastEvent", snapshotMetrics.getMillisSinceLastEvent());
      metricService.recordMetric("aggregator_snapshot_queueTotalCapacity", snapshotMetrics.getQueueTotalCapacity());
      metricService.recordMetric(
          "aggregator_snapshot_queueRemainingCapacity", snapshotMetrics.getQueueRemainingCapacity());
      log.info("Snapshot=> Last event seen: {}", snapshotMetrics.getLastEvent());
      metricService.recordMetric(
          "aggregator_snapshot_totalNumberOfEventsSeen", snapshotMetrics.getTotalNumberOfEventsSeen());
      metricService.recordMetric(
          "aggregator_snapshot_currentQueueSizeInBytes", snapshotMetrics.getCurrentQueueSizeInBytes());
      metricService.recordMetric("aggregator_snapshot_snapshotRunning", snapshotMetrics.isSnapshotRunning() ? 1 : 0);
      metricService.recordMetric(
          "aggregator_snapshot_snapshotCompleted", snapshotMetrics.isSnapshotCompleted() ? 1 : 0);
      metricService.recordMetric(
          "aggregator_snapshot_snapshotDurationInSeconds", snapshotMetrics.getSnapshotDurationInSeconds());
    } else {
      log.info("Snapshot metrics are not yet available.");
    }
  }
}