package io.harness.cvng.metrics;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

public interface CVNGMetricsUtils {
  String METRIC_LABEL_PREFIX = "metricsLabel_";
  String ORCHESTRATOR_QUEUE_SIZE = "orchestrator_queue_size";
  // time taken metrics are tracked from the time when task becomes eligible to finish.
  String VERIFICATION_JOB_INSTANCE_EXTRA_TIME = "verification_job_instance_extra_time";
  String DATA_COLLECTION_TASK_TOTAL_TIME = "data_collection_task_total_time";
  String DATA_COLLECTION_TASK_WAIT_TIME = "data_collection_task_wait_time";
  String DATA_COLLECTION_TASK_RUNNING_TIME = "data_collection_task_running_time";
  String LEARNING_ENGINE_TASK_TOTAL_TIME = "learning_engine_task_total_time";
  String LEARNING_ENGINE_TASK_WAIT_TIME = "learning_engine_task_wait_time";
  String LEARNING_ENGINE_TASK_RUNNING_TIME = "learning_engine_task_running_time";
  static String getLearningEngineTaskStatusMetricName(LearningEngineTask.ExecutionStatus executionStatus) {
    return String.format("learning_engine_task_%s_count", executionStatus.toString().toLowerCase());
  }

  static String getVerificationJobInstanceStatusMetricName(VerificationJobInstance.ExecutionStatus executionStatus) {
    return String.format("verification_job_instance_%s_count", executionStatus.toString().toLowerCase());
  }

  static String getVerificationJobInstanceStatusMetricName(ActivityVerificationStatus activityVerificationStatus) {
    return String.format("verification_job_instance_%s_count", activityVerificationStatus.toString().toLowerCase());
  }
  static String getDataCollectionTaskStatusMetricName(DataCollectionExecutionStatus executionStatus) {
    return String.format("data_collection_task_%s_count", executionStatus.toString().toLowerCase());
  }
}