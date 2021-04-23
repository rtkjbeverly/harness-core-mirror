package io.harness.platform;

import io.harness.govern.ProviderModule;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.IndexManager;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NGAuditServiceRegistrars;
import io.harness.serializer.NotificationRegistrars;
import io.harness.serializer.morphia.ResourceGroupSerializer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.converters.TypeConverter;

public class InspectCommand<T extends io.dropwizard.Configuration> extends ConfiguredCommand<T> {
  public static final String PRIMARY_DATASTORE = "primaryDatastore";
  private final Class<T> configurationClass;

  public InspectCommand(Application<T> application) {
    super("inspect", "Parses and validates the configuration file");
    this.configurationClass = application.getConfigurationClass();
  }

  @Override
  protected Class<T> getConfigurationClass() {
    return this.configurationClass;
  }
  @Override
  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) throws Exception {
    PlatformConfiguration appConfig = (PlatformConfiguration) configuration;
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }
    });
    List<Module> notificationModules = new ArrayList<>(modules);
    notificationModules.add(getMongoConfigModule(
        appConfig.getNotificationServiceConfig().getMongoConfig(), NotificationRegistrars.morphiaRegistrars));
    Injector injector = Guice.createInjector(notificationModules);
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named(PRIMARY_DATASTORE)));

    List<Module> resourceGroupModules = new ArrayList<>(modules);
    resourceGroupModules.add(getMongoConfigModule(
        appConfig.getResoureGroupServiceConfig().getMongoConfig(), ResourceGroupSerializer.morphiaRegistrars));
    injector = Guice.createInjector(resourceGroupModules);
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named(PRIMARY_DATASTORE)));

    List<Module> auditModules = new ArrayList<>(modules);
    auditModules.add(getMongoConfigModule(
        appConfig.getAuditServiceConfig().getMongoConfig(), NGAuditServiceRegistrars.morphiaRegistrars));
    injector = Guice.createInjector(auditModules);
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named(PRIMARY_DATASTORE)));
  }

  private Module getMongoConfigModule(
      MongoConfig mongoConfig, ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars) {
    return new AbstractModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder().build();
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return mongoConfig.toBuilder().indexManagerMode(IndexManager.Mode.INSPECT).build();
      }

      @Provides
      @Singleton
      public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().addAll(morphiaRegistrars).build();
      }
    };
  }
}