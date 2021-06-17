package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.persistence.HPersistence.returnNewOptions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.mongo.MongoUtils;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.ScheduledTriggerJob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@Singleton
@TargetModule(HarnessModule._390_DB_MIGRATION)
@OwnedBy(CDC)
public class AddEnableIteratorsToTriggers implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  private static final String LOG_IDENTIFIER = "[MIGRATE_TRIGGERS_TO_USE_ITERATOR_FRAMEWORK]: ";
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public void migrate() {
    log.info(LOG_IDENTIFIER + "Running migration to add enableNextIterations to trigger config");
    int count = 0;
    try (HIterator<Trigger> triggerHIterator = new HIterator<>(wingsPersistence.createQuery(Trigger.class)
                                                                   .field(Trigger.TriggerKeys.triggerConditionType)
                                                                   .equal("SCHEDULED")
                                                                   .fetch())) {
      while (triggerHIterator.hasNext()) {
        migrateTrigger(triggerHIterator.next());
        count++;
      }
    } catch (Exception e) {
      log.error(LOG_IDENTIFIER + "Could not run migration to add enableNextIterations to triggers ", e);
    }
    log.info(LOG_IDENTIFIER + "Updated {} trigger records", count);
  }

  private void migrateTrigger(Trigger trigger) {
    try {
      log.info(
          LOG_IDENTIFIER + "Updating trigger with id {} for accountId {}", trigger.getUuid(), trigger.getAccountId());
      // Pause the current scheduler job
      jobScheduler.pauseJob(trigger.getUuid(), ScheduledTriggerJob.GROUP);

      // Fetch the nextFireTime from Quartz
      trigger.setNextIterations(new ArrayList<>());
      List<Long> nextFireTime = trigger.recalculateNextIterations(Trigger.TriggerKeys.nextIterations, true, 0);
      if (!nextFireTime.isEmpty()) {
        // It automatically adds current timestamp when list is empty. This is not desired
        nextFireTime = nextFireTime.subList(1, nextFireTime.size());
      }

      // Update the current trigger to use the IteratorsFramework
      Query<Trigger> query = wingsPersistence.createQuery(Trigger.class)
                                 .filter(Trigger.TriggerKeys.uuid, trigger.getUuid())
                                 .filter(Trigger.TriggerKeys.accountId, trigger.getAccountId());

      UpdateOperations<Trigger> operations = wingsPersistence.createUpdateOperations(Trigger.class);
      MongoUtils.setUnset(operations, Trigger.TriggerKeys.nextIterations, nextFireTime);
      wingsPersistence.findAndModify(query, operations, returnNewOptions);
      log.info(
          LOG_IDENTIFIER + "Updated trigger with id {} for accountId {}", trigger.getUuid(), trigger.getAccountId());
    } catch (Exception e) {
      // Resume the current scheduler job
      jobScheduler.resumeJob(trigger.getUuid(), ScheduledTriggerJob.GROUP);
      log.error(LOG_IDENTIFIER + "Could not update trigger with id " + trigger.getUuid() + " for accountId "
              + trigger.getAccountId(),
          e);
    }
  }
}