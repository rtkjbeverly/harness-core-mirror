package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.changehandlers.OrganizationsChangeDataHandler;
import io.harness.changehandlers.TagsInfoCDChangeDataHandler;
import io.harness.ng.core.entities.Organization;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class OrganizationEntity implements CDCEntity<Organization> {
  @Inject private OrganizationsChangeDataHandler organizationsChangeDataHandler;

  @Inject private TagsInfoCDChangeDataHandler tagsInfoCDChangeDataHandler;

  @Override
  public ChangeHandler getChangeHandler(String handlerClass) {
    if (handlerClass.contentEquals("TagsInfoCD")) {
      return tagsInfoCDChangeDataHandler;
    }
    return organizationsChangeDataHandler;
  }

  @Override
  public Class<? extends PersistentEntity> getSubscriptionEntity() {
    return Organization.class;
  }
}