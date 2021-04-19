package io.harness;

import io.harness.accesscontrol.DecisionMorphiaRegistrar;
import io.harness.accesscontrol.acl.ACLModule;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.ScopeModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

@OwnedBy(HarnessTeam.PL)
public class DecisionModule extends AbstractModule {
  private static DecisionModule instance;
  private final DecisionModuleConfiguration decisionModuleConfiguration;

  public DecisionModule(DecisionModuleConfiguration decisionModuleConfiguration) {
    this.decisionModuleConfiguration = decisionModuleConfiguration;
  }

  public static synchronized DecisionModule getInstance(DecisionModuleConfiguration decisionModuleConfiguration) {
    if (instance == null) {
      instance = new DecisionModule(decisionModuleConfiguration);
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(DecisionMorphiaRegistrar.class);
    install(ScopeModule.getInstance());
    install(ACLModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleService.class);
    requireBinding(RoleAssignmentService.class);
  }
}