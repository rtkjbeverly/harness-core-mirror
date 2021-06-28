package io.harness.ng.accesscontrol.migrations;

import io.harness.NGConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.migrations.services.AccessControlMigrationService;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.OrganizationService;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationJob implements Managed {
  private final OrganizationService organizationService;
  private final AccessControlMigrationService migrationService;
  private final MongoTemplate mongoTemplate;
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("access-control-migration-job").build());

  @Override
  public void start() {
    // this is a temporary change, this code should be removed after it runs in all the environments
    try {
      mongoTemplate.dropCollection("accessControlMigrations");
      mongoTemplate.dropCollection("accessControlPreferences");
    } catch (Exception exception) {
      log.error("Error while dropping accessControlMigrations/accessControlPreferences collection", exception);
    }

    executorService.scheduleWithFixedDelay(this::run, 1, 30, TimeUnit.MINUTES);
  }

  private void run() {
    List<String> accountsToMigrate = new ArrayList<>(getAccountsToMigrate());
    Collections.shuffle(accountsToMigrate);
    for (String accountId : accountsToMigrate) {
      try {
        log.info("Access control migration job started for account : {}", accountId);
        Stopwatch stopwatch = Stopwatch.createStarted();
        migrationService.migrate(accountId);
        log.info("Access control migration job finished for account : {} in {} minutes", accountId,
            stopwatch.elapsed(TimeUnit.MINUTES));
      } catch (Exception ex) {
        log.error("Access control migration job failed", ex);
      }
    }
  }

  private List<String> getAccountsToMigrate() {
    return organizationService
        .list(Criteria.where(OrganizationKeys.identifier).is(NGConstants.DEFAULT_ORG_IDENTIFIER), Pageable.unpaged())
        .map(Organization::getAccountIdentifier)
        .getContent();
  }

  @Override
  public void stop() throws InterruptedException {
    executorService.shutdownNow();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }
}