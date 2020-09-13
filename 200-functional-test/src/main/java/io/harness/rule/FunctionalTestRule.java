package io.harness.rule;

import static io.harness.cache.CacheBackend.NOOP;
import static io.harness.mongo.MongoModule.defaultMongoClientOptions;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import graphql.GraphQL;
import io.dropwizard.Configuration;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsModule;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.cache.CacheConfig;
import io.harness.cache.CacheModule;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.configuration.ConfigurationType;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.event.EventsModule;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.grpc.DelegateServiceGrpcClientModule;
import io.harness.grpc.GrpcServiceConfigurationModule;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.grpc.server.Connector;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.QueryFactory;
import io.harness.organizationmanagerclient.OrganizationManagerClientConfig;
import io.harness.persistence.HPersistence;
import io.harness.rest.RestResponse;
import io.harness.scm.ScmSecret;
import io.harness.security.AsymmetricDecryptor;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.service.DelegateServiceModule;
import io.harness.testframework.framework.ManagerExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import lombok.Getter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.AuthModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GraphQLModule;
import software.wings.app.IndexMigratorModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.SSOModule;
import software.wings.app.SearchModule;
import software.wings.app.SignupModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.WingsPersistenceConfig;
import software.wings.app.YamlModule;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.search.framework.ElasticsearchConfig;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.EventEmitter;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.GenericType;

public class FunctionalTestRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  private ClosingFactory closingFactory;

  public FunctionalTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  protected AdvancedDatastore datastore;
  private ExecutorService executorService = new CurrentThreadExecutor();
  public static final String alpnJar =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  public static final String alpn = "/home/jenkins/maven-repositories/0/";
  @Getter private GraphQL graphQL;

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ManagerExecutor.ensureManager(AbstractFunctionalTest.class, alpn, alpnJar);

    RestResponse<MongoConfig> mongoConfigRestResponse =
        Setup.portal()
            .queryParam("configurationType", ConfigurationType.MONGO)
            .get("/health/configuration")
            .as(new GenericType<RestResponse<MongoConfig>>() {}.getType());

    String mongoUri =
        new AsymmetricDecryptor(new ScmSecret()).decryptText(mongoConfigRestResponse.getResource().getEncryptedUri());

    MongoClientURI clientUri = new MongoClientURI(mongoUri, MongoClientOptions.builder(defaultMongoClientOptions));
    String dbName = clientUri.getDatabase();

    MongoClient mongoClient = new MongoClient(clientUri);
    closingFactory.addServer(mongoClient);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, dbName);
    datastore.setQueryFactory(new QueryFactory());

    RestResponse<ElasticsearchConfig> elasticsearchConfigRestResponse =
        Setup.portal()
            .queryParam("configurationType", ConfigurationType.ELASTICSEARCH)
            .get("/health/configuration")
            .as(new GenericType<RestResponse<ElasticsearchConfig>>() {}.getType());

    String elasticsearchUri = new AsymmetricDecryptor(new ScmSecret())
                                  .decryptText(elasticsearchConfigRestResponse.getResource().getEncryptedUri());
    String elasticsearchIndexSuffix = elasticsearchConfigRestResponse.getResource().getIndexSuffix();
    ElasticsearchConfig elasticsearchConfig =
        ElasticsearchConfig.builder().uri(elasticsearchUri).indexSuffix(elasticsearchIndexSuffix).build();

    RestResponse<Boolean> searchEnabledRestResponse =
        Setup.portal()
            .queryParam("configurationType", ConfigurationType.SEARCH_ENABLED)
            .get("/health/configuration")
            .as(new GenericType<RestResponse<Boolean>>() {}.getType());

    boolean isSearchEnabled = searchEnabledRestResponse.getResource();

    Configuration configuration = getConfiguration(mongoUri, elasticsearchConfig, isSearchEnabled);

    io.harness.threading.ExecutorModule.getInstance().setExecutorService(executorService);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));

    modules.add(new KryoModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(ManagerRegistrars.kryoRegistrars)
            .add(CvNextGenCommonsBeansKryoRegistrar.class)
            .add(TestPersistenceKryoRegistrar.class)
            .build();
      }
    });

    modules.add(new ProviderModule() {
      @Override
      public void configure() {
        install(new SpringModule(BeanFactoryProvider.from(TimeoutEnginePersistenceConfig.class,
            OrchestrationPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class,
            ConnectorPersistenceConfig.class, WingsPersistenceConfig.class)));
      }

      @Provides
      @Named("locksDatabase")
      @Singleton
      String databaseNameProvider() {
        return dbName;
      }

      @Provides
      @Named("locksMongoClient")
      @Singleton
      public MongoClient locksMongoClient(ClosingFactory closingFactory) {
        return mongoClient;
      }

      @Provides
      @Named("primaryDatastore")
      @Singleton
      AdvancedDatastore datastore() {
        return datastore;
      }
    });

    CacheModule cacheModule = new CacheModule(CacheConfig.builder()
                                                  .cacheBackend(NOOP)
                                                  .disabledCaches(new HashSet<>())
                                                  .cacheNamespace("harness-cache")
                                                  .build());
    modules.addAll(0, cacheModule.cumulativeDependencies());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EventEmitter.class).toInstance(mock(EventEmitter.class));
        bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
        bind(MetricRegistry.class);
        bind(CommandLibraryServiceHttpClient.class).toInstance(mock(CommandLibraryServiceHttpClient.class));
      }
    });
    modules.add(new LicenseModule());
    modules.add(new ValidationModule(validatorFactory));
    modules.add(new DelegateServiceModule());
    modules.add(OrchestrationStepsModule.getInstance());
    modules.add(new WingsModule((MainConfiguration) configuration));
    modules.add(new IndexMigratorModule());
    modules.add(new YamlModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule((MainConfiguration) configuration));
    modules.add(new GraphQLModule());
    modules.add(new SSOModule());
    modules.add(new SignupModule());
    modules.add(new SearchModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    modules.add(new AuthModule());
    modules.add(new ManagerQueueModule());
    modules.add(new ManagerGrpcClientModule(
        ManagerGrpcClientModule.Config.builder().target("localhost:9880").authority("localhost").build()));

    modules.add(new GrpcServiceConfigurationModule(((MainConfiguration) configuration).getGrpcServerConfig(),
        ((MainConfiguration) configuration).getPortal().getJwtNextGenManagerSecret()));
    modules.add(new DelegateServiceGrpcClientModule(
        ((MainConfiguration) configuration).getPortal().getJwtNextGenManagerSecret()));
    return modules;
  }

  protected Configuration getConfiguration(
      String mongoUri, ElasticsearchConfig elasticsearchConfig, boolean isSearchEnabled) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setUrl("PORTAL_URL");
    configuration.getPortal().setVerificationUrl("VERIFICATION_PATH");
    configuration.getPortal().setJwtNextGenManagerSecret(
        "IC04LYMBf1lDP5oeY4hupxd4HJhLmN6azUku3xEbeE3SUx5G3ZYzhbiwVtK4i7AmqyU9OZkwB4v8E9qM");
    GrpcServerConfig grpcServerConfig = new GrpcServerConfig();
    grpcServerConfig.setConnectors(Arrays.asList(
        Connector.builder().port(9880).secure(true).keyFilePath("key.pem").certFilePath("cert.pem").build()));
    configuration.setGrpcServerConfig(grpcServerConfig);
    configuration.setMongoConnectionFactory(MongoConfig.builder().uri(mongoUri).build());
    configuration.setElasticsearchConfig(elasticsearchConfig);
    configuration.setSearchEnabled(isSearchEnabled);
    configuration.setSegmentConfig(
        SegmentConfig.builder().enabled(false).apiKey("dummy_api_key").url("dummy_url").build());
    configuration.setOrganizationManagerClientConfig(
        OrganizationManagerClientConfig.builder().baseUrl("http://localhost:7457/").build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    return configuration;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    System.setProperty("javax.cache.spi.CachingProvider", "com.hazelcast.cache.HazelcastCachingProvider");
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }

    final QueryLanguageProvider<GraphQL> instance =
        injector.getInstance(Key.get(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}));
    graphQL = instance.getPrivateGraphQL();

    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
