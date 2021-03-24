package io.harness.platform.notification;

import static io.harness.AuthorizationServiceHeader.NOTIFICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.notification.constant.NotificationServiceConstants.MAILSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.MSTEAMSSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.PAGERDUTYSERVICE;
import static io.harness.notification.constant.NotificationServiceConstants.SLACKSERVICE;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.govern.ProviderModule;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.queue.NGMongoQueueConsumer;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.UserClientModule;
import io.harness.notification.SmtpConfig;
import io.harness.notification.entities.MongoNotificationRequest;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.eventbackbone.MongoMessageConsumer;
import io.harness.notification.modules.SmtpConfigClientModule;
import io.harness.notification.service.ChannelServiceImpl;
import io.harness.notification.service.MSTeamsServiceImpl;
import io.harness.notification.service.MailServiceImpl;
import io.harness.notification.service.NotificationServiceImpl;
import io.harness.notification.service.NotificationSettingsServiceImpl;
import io.harness.notification.service.NotificationTemplateServiceImpl;
import io.harness.notification.service.PagerDutyServiceImpl;
import io.harness.notification.service.SeedDataPopulaterServiceImpl;
import io.harness.notification.service.SlackServiceImpl;
import io.harness.notification.service.api.ChannelService;
import io.harness.notification.service.api.NotificationService;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.notification.service.api.NotificationTemplateService;
import io.harness.notification.service.api.SeedDataPopulaterService;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.platform.PlatformConfiguration;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NotificationRegistrars;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.threading.ExecutorModule;
import io.harness.usergroups.UserGroupClientModule;
import io.harness.version.VersionModule;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
@OwnedBy(PL)
public class NotificationServiceModule extends AbstractModule {
  PlatformConfiguration appConfig;

  public NotificationServiceModule(PlatformConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return Suppliers.memoize(() -> getDelegateCallbackToken(delegateServiceGrpcClient, appConfig));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, PlatformConfiguration appConfig) {
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("!!!custom")
                                  .setConnection(appConfig.getNotificationServiceConfig().getMongoConfig().getUri())
                                  .build())
            .build());
    log.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  public void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(NotificationRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(NotificationRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getNotificationServiceConfig().getMongoConfig();
      }
    });

    install(ExecutorModule.getInstance());
    bind(ManagedScheduledExecutorService.class)
        .annotatedWith(Names.named("delegate-response"))
        .toInstance(new ManagedScheduledExecutorService("delegate-response"));
    bind(PlatformConfiguration.class).toInstance(appConfig);
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    bind(HPersistence.class).to(MongoPersistence.class);
    install(DelegateServiceDriverModule.getInstance());
    install(new DelegateServiceDriverGrpcClientModule(appConfig.getPlatformSecrets().getNgManagerServiceSecret(),
        this.appConfig.getNotificationServiceConfig().getGrpcClientConfig().getTarget(),
        this.appConfig.getNotificationServiceConfig().getGrpcClientConfig().getAuthority()));

    install(VersionModule.getInstance());
    install(new ValidationModule(getValidatorFactory()));

    install(new NotificationPersistenceModule());

    install(new UserGroupClientModule(appConfig.getRbacServiceConfig(),
        appConfig.getPlatformSecrets().getNgManagerServiceSecret(), NOTIFICATION_SERVICE.getServiceId()));
    install(new UserClientModule(appConfig.getServiceHttpClientConfig(),
        appConfig.getPlatformSecrets().getNgManagerServiceSecret(), NOTIFICATION_SERVICE.getServiceId()));
    bind(ChannelService.class).to(ChannelServiceImpl.class);
    install(new SmtpConfigClientModule(
        appConfig.getServiceHttpClientConfig(), appConfig.getPlatformSecrets().getNgManagerServiceSecret()));
    bind(NotificationSettingsService.class).to(NotificationSettingsServiceImpl.class);
    bind(SeedDataPopulaterService.class).to(SeedDataPopulaterServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(MAILSERVICE)).to(MailServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(SLACKSERVICE)).to(SlackServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(PAGERDUTYSERVICE)).to(PagerDutyServiceImpl.class);
    bind(ChannelService.class).annotatedWith(Names.named(MSTEAMSSERVICE)).to(MSTeamsServiceImpl.class);
    bind(NotificationService.class).to(NotificationServiceImpl.class);
    bind(NotificationTemplateService.class).to(NotificationTemplateServiceImpl.class);
    bindMessageConsumer();
    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  private void bindMessageConsumer() {
    log.info("Using Mongo as the message broker");
    bind(MessageConsumer.class).to(MongoMessageConsumer.class);
  }

  @Provides
  @Singleton
  SmtpConfig getSmtpConfig() {
    return appConfig.getNotificationServiceConfig().getSmtpConfig();
  }

  @Provides
  @Singleton
  QueueConsumer<MongoNotificationRequest> getQueueConsumer(MongoTemplate mongoTemplate) {
    return new NGMongoQueueConsumer<>(MongoNotificationRequest.class, ofSeconds(5), new ArrayList<>(), mongoTemplate);
  }
}