package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;

import static software.wings.security.PermissionAttribute.Action.EXECUTE_WORKFLOW;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_WORKFLOW_ROLLBACK;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CDC)
@Slf4j
public class ExecuteWorkflowRollbackActionMigration implements Migration {
  private static final String DEBUG_LINE = "EXECUTE_WORKFLOW_ROLLBACK_MIGRATION";
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("{}: Starting migration", DEBUG_LINE);

    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.appPermissions).exists().fetch())) {
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        if (isNotEmpty(userGroup.getAppPermissions())) {
          Set<AppPermission> appPermissions = new HashSet<>();
          for (AppPermission appPermission : userGroup.getAppPermissions()) {
            if (isNotEmpty(appPermission.getActions())
                && (appPermission.getPermissionType() == ALL_APP_ENTITIES
                    || appPermission.getPermissionType() == DEPLOYMENT)) {
              Set<Action> actionSet = new HashSet<>();
              appPermission.getActions().forEach(action -> {
                if (action != null && action.equals(EXECUTE_WORKFLOW)) {
                  actionSet.add(EXECUTE_WORKFLOW_ROLLBACK);
                }
                actionSet.add(action);
              });
              appPermission.setActions(actionSet);
            }
            appPermissions.add(appPermission);
          }
          UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
          setUnset(operations, UserGroupKeys.appPermissions, appPermissions);
          wingsPersistence.update(userGroup, operations);
          log.info("{}: Migrated userGroupId: {}", DEBUG_LINE, userGroup.getUuid());
        }
      }
    } catch (Exception e) {
      log.error("{}: Could not run Migration", DEBUG_LINE, e);
    }
  }
}