package io.harness.workers.background.critical.iterator;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.EXPIRED;
import static io.harness.beans.ExecutionStatus.PREPARING;
import static io.harness.beans.ExecutionStatus.flowingStatuses;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.interrupts.ExecutionInterruptType.MARK_EXPIRED;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.FeatureName;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachineExecutor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class WorkflowExecutionMonitorHandler implements Handler<WorkflowExecution> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ExecutorService executorService;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MorphiaPersistenceProvider<WorkflowExecution> persistenceProvider;

  private static final Duration INACTIVITY_TIMEOUT = Duration.ofSeconds(50);
  private static final Duration EXPIRE_THRESHOLD = Duration.ofMinutes(10);

  public void registerIterators() {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofSeconds(10))
                                      .poolSize(5)
                                      .name("WorkflowExecutionMonitor")
                                      .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options, WorkflowExecution.class,
        MongoPersistenceIterator.<WorkflowExecution, MorphiaFilterExpander<WorkflowExecution>>builder()
            .clazz(WorkflowExecution.class)
            .fieldName(WorkflowExecutionKeys.nextIteration)
            .filterExpander(q -> q.field(WorkflowExecutionKeys.status).in(flowingStatuses()))
            .targetInterval(Duration.ofMinutes(1))
            .acceptableNoAlertDelay(Duration.ofSeconds(30))
            .handler(this)
            .schedulingType(SchedulingType.REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(WorkflowExecution entity) {
    try {
      // logic to expire workflowExecution if its stuck in preparing state for more than 10 mins
      if (featureFlagService.isEnabled(FeatureName.NAS_SUPPORT, entity.getAccountId())) {
        if (entity.getStatus() == PREPARING
            && System.currentTimeMillis() - entity.getStartTs() > EXPIRE_THRESHOLD.toMillis()) {
          updateStartStatusAndUnsetMessage(entity.getAppId(), entity.getUuid(), EXPIRED);
          return;
        }
      }
      boolean hasActiveStates = false;
      try (HIterator<StateExecutionInstance> stateExecutionInstances =
               new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                   .filter(StateExecutionInstanceKeys.appId, entity.getAppId())
                                   .filter(StateExecutionInstanceKeys.executionUuid, entity.getUuid())
                                   .field(StateExecutionInstanceKeys.status)
                                   .in(flowingStatuses())
                                   .fetch())) {
        hasActiveStates = stateExecutionInstances.hasNext();
        while (stateExecutionInstances.hasNext()) {
          StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
          if (stateExecutionInstance.getExpiryTs() > System.currentTimeMillis()) {
            continue;
          }

          logger.info("Expired StateExecutionInstance found: {}", stateExecutionInstance.getUuid());
          ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                      .executionInterruptType(MARK_EXPIRED)
                                                      .appId(stateExecutionInstance.getAppId())
                                                      .executionUuid(stateExecutionInstance.getExecutionUuid())
                                                      .stateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                      .build();

          executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
        }
      } catch (WingsException exception) {
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        logger.error("Error in cleaning up the workflow execution {}", entity.getUuid(), e);
      }

      if (!hasActiveStates) {
        logger.warn("WorkflowExecution {} is in non final state, but there is no active state execution for it.",
            entity.getUuid());

        final StateExecutionInstance stateExecutionInstance =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(StateExecutionInstanceKeys.appId, entity.getAppId())
                .filter(StateExecutionInstanceKeys.executionUuid, entity.getUuid())
                .field(StateExecutionInstanceKeys.notifyId)
                .doesNotExist()
                .field(StateExecutionInstanceKeys.callback)
                .exists()
                .order(Sort.descending(StateExecutionInstanceKeys.lastUpdatedAt))
                .get();

        if (stateExecutionInstance == null) {
          logger.error("Workflow execution stuck, but we cannot find good state to callback from. This is so wrong!");
          return;
        }

        if (stateExecutionInstance.getLastUpdatedAt() > System.currentTimeMillis() - INACTIVITY_TIMEOUT.toMillis()) {
          logger.warn("WorkflowExecution {} last callbackable state {} is very recent."
                  + "Lets give more time to the system it might be just in the middle of things.",
              entity.getUuid(), stateExecutionInstance.getUuid());
          return;
        }

        final ExecutionContextImpl executionContext =
            stateMachineExecutor.getExecutionContext(stateExecutionInstance.getAppId(),
                stateExecutionInstance.getExecutionUuid(), stateExecutionInstance.getUuid());

        try (AutoLogContext ignore = executionContext.autoLogContext()) {
          boolean expired = entity.getCreatedAt() < System.currentTimeMillis() - WorkflowExecution.EXPIRY.toMillis();
          // We lost the eventual exception, but its better than doing nothing
          ExecutionStatus finalStatus = expired ? EXPIRED : ERROR;
          logger.info("[WorkflowStateUpdate] Executing StateCallBack with status: {}", finalStatus);
          stateMachineExecutor.executeCallback(executionContext, stateExecutionInstance, finalStatus, null);
        }
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error in monitoring the workflow execution {}", entity.getUuid());
    }
  }

  private void updateStartStatusAndUnsetMessage(String appId, String workflowExecutionId, ExecutionStatus status) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                         .filter(WorkflowExecutionKeys.status, PREPARING);

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set(WorkflowExecutionKeys.status, status)
                                                        .set(WorkflowExecutionKeys.startTs, System.currentTimeMillis())
                                                        .unset(WorkflowExecutionKeys.message);

    wingsPersistence.findAndModify(query, updateOps, HPersistence.returnNewOptions);
  }
}
