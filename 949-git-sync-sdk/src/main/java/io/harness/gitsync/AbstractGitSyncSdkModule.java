package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.grpc.client.GrpcClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractGitSyncSdkModule extends AbstractModule {
  @Override
  protected void configure() {
    install(GitSyncSdkModule.getInstance());
    bind(Producer.class)
        .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
        .toInstance(RedisProducer.of(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH,
            getGitSyncSdkConfiguration().getEventsRedisConfig(),
            EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_MAX_TOPIC_SIZE));
  }

  public abstract GitSyncSdkConfiguration getGitSyncSdkConfiguration();

  @Provides
  @Singleton
  @Named("GitSyncGrpcClientConfig")
  public GrpcClientConfig grpcClientConfig() {
    return getGitSyncSdkConfiguration().getGrpcClientConfig();
  }

  @Provides
  @Singleton
  @Named("GitSyncSortOrder")
  public Supplier<List<EntityType>> getSortOrder() {
    return getGitSyncSdkConfiguration().getGitSyncSortOrder();
  }
}