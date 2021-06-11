package io.harness.eventsframework.monitor;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.impl.redis.RedisUtils;
import io.harness.metrics.modules.MetricsModule;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import service.RedisStreamsMetricsAggregator;
import service.RedisStreamsMetricsAggregatorImpl;

@OwnedBy(HarnessTeam.PL)
public class EventsFrameworkMonitorModule extends AbstractModule {
  private final EventsFrameworkMonitorConfiguration appConfig;

  public EventsFrameworkMonitorModule(EventsFrameworkMonitorConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    bind(RedisStreamsMetricsAggregator.class).to(RedisStreamsMetricsAggregatorImpl.class);
    install(new MetricsModule());
  }

  @Provides
  @Singleton
  RedissonClient getRedissonClient() {
    RedisConfig redisConfig = this.appConfig.getEventsFrameworkConfiguration().getRedisConfig();
    return RedisUtils.getClient(redisConfig);
  }

  @Provides
  @Singleton
  RedisClient getLowLevelRedisClient() {
    RedisConfig redisConfig = this.appConfig.getEventsFrameworkConfiguration().getRedisConfig();
    return RedisUtils.getLowLevelClient(redisConfig);
  }
}