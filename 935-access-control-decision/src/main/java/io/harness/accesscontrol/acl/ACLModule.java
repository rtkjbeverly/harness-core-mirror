package io.harness.accesscontrol.acl;

import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.daos.ACLDAOImpl;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.acl.services.ACLServiceImpl;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PL)
public class ACLModule extends AbstractModule {
  private static ACLModule instance;

  public static ACLModule getInstance() {
    if (instance == null) {
      instance = new ACLModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ACLService.class).to(ACLServiceImpl.class);
    bind(ACLDAO.class).to(ACLDAOImpl.class);
  }
}