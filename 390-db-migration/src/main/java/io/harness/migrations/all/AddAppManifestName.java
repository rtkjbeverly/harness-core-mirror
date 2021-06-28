package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.ExpressionEvaluator;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ApplicationManifestServiceImpl;
import software.wings.service.intfc.ServiceResourceService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@Singleton
@TargetModule(HarnessModule._390_DB_MIGRATION)
@OwnedBy(CDC)
public class AddAppManifestName implements Migration {
  private static final String DEBUG_LINE = "[APP_MANIFEST_NAME_ADDITION_MIGRATION]: ";
  @Inject private WingsPersistence wingsPersistence;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public void migrate() {
    log.info(String.join(DEBUG_LINE, "Starting Migration For Adding application manifest name"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        log.info(String.join(DEBUG_LINE, " Starting Migration For Adding application manifest name for account ",
            account.getAccountName()));
        migrateApplicationManifestsForAccount(account);
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching Accounts"));
    }
  }

  private void migrateApplicationManifestsForAccount(Account account) {
    try (HIterator<ApplicationManifest> applicationManifests =
             new HIterator<>(wingsPersistence.createQuery(ApplicationManifest.class)
                                 .filter(ApplicationManifest.ApplicationManifestKeys.accountId, account.getUuid())
                                 .fetch())) {
      log.info(String.join(DEBUG_LINE, " Fetching application manifests for account", account.getAccountName(),
          "with Id", account.getUuid()));
      while (applicationManifests.hasNext()) {
        migrateApplicationManifest(applicationManifests.next());
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching app manifests with Account ", account.getUuid()));
    }
  }

  @VisibleForTesting
  void migrateApplicationManifest(ApplicationManifest applicationManifest) {
    try {
      if (Boolean.TRUE.equals(applicationManifest.getPollForChanges())
          && applicationManifest.getHelmChartConfig() != null) {
        String chartName = applicationManifest.getHelmChartConfig().getChartName();
        String serviceName =
            serviceResourceService.getName(applicationManifest.getAppId(), applicationManifest.getServiceId());
        if (serviceName != null) {
          UpdateOperations<ApplicationManifest> updateOperations =
              wingsPersistence.createUpdateOperations(ApplicationManifest.class)
                  .set(ApplicationManifest.ApplicationManifestKeys.name, serviceName + "_" + chartName);
          if (ExpressionEvaluator.containsVariablePattern(chartName)) {
            updateOperations.set(ApplicationManifest.ApplicationManifestKeys.validationMessage,
                ApplicationManifestServiceImpl.VARIABLE_EXPRESSIONS_ERROR);
          }
          wingsPersistence.update(applicationManifest, updateOperations);
          log.info("Successfully added name {} to application manifest with id {}", serviceName + "_" + chartName,
              applicationManifest.getUuid());
          wingsPersistence.updateField(
              Service.class, applicationManifest.getServiceId(), Service.ServiceKeys.artifactFromManifest, true);
        } else {
          log.info("Orphan app manifest with id {} of non existent service id {} found", applicationManifest.getUuid(),
              applicationManifest.getServiceId());
        }
      }
    } catch (RuntimeException e) {
      log.error(String.join(DEBUG_LINE, applicationManifest.getUuid(), "Failed With RuntimeException", e.getMessage()));
    } catch (Exception e) {
      log.error(String.join(DEBUG_LINE, applicationManifest.getUuid(), "Failed With Exception", e.getMessage()));
    }
  }
}