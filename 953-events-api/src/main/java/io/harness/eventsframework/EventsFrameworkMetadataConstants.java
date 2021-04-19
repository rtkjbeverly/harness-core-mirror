package io.harness.eventsframework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public final class EventsFrameworkMetadataConstants {
  public static final String ENTITY_TYPE = "entityType";

  public static final String ACTION = "action";
  public static final String CREATE_ACTION = "create";
  public static final String RESTORE_ACTION = "restore";
  public static final String UPDATE_ACTION = "update";
  public static final String UPSERT_ACTION = "upsert";
  public static final String DELETE_ACTION = "delete";
  public static final String FLUSH_CREATE_ACTION = "flushCreate";

  public static final String PROJECT_ENTITY = "project";
  public static final String ORGANIZATION_ENTITY = "organization";
  public static final String CONNECTOR_ENTITY = "connector";
  public static final String SECRET_ENTITY = "secret";

  public static final String PIPELINE_ENTITY = "pipeline";

  public static final String SERVICE_ENTITY = "service";
  public static final String ENVIRONMENT_ENTITY = "environment";

  public static final String RESOURCE_GROUP = "resourcegroup";
  public static final String USER_GROUP = "usergroup";
  // deprecated, use setupusage and entityActivity channel.
  public static final String SETUP_USAGE_ENTITY = "setupUsage";
  public static final String ACCOUNT_ENTITY = "account";

  public static final String REFERRED_ENTITY_TYPE = "referredEntityType";
}