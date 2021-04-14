package io.harness.ng.eventsframework;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_TRIGGER_EVENT_DATA;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_TRIGGER_EVENT_DATA_MAX_TOPIC_SIZE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor
public class EventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_TRIGGER_EVENT_DATA))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_CRUD, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.FEATURE_FLAG_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.FEATURE_FLAG_STREAM, NG_MANAGER.getServiceId(),
              redisConfig, EventsFrameworkConstants.FEATURE_FLAG_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.FEATURE_FLAG_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.SETUP_USAGE, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.SETUP_USAGE, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.SETUP_USAGE_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.SETUP_USAGE_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_ACTIVITY, redisConfig,
              EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_ACTIVITY, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ENTITY_ACTIVITY_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH, NG_MANAGER.getServiceId(),
              redisConfig, EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_READ_BATCH_SIZE));
      // todo(abhinav): move this to git sync manager if it is carved out.
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.GIT_CONFIG_STREAM, redisConfig,
              EventsFrameworkConstants.GIT_CONFIG_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.USERMEMBERSHIP, redisConfig,
              EventsFrameworkConstants.DEFAULT_TOPIC_SIZE, MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_TRIGGER_EVENT_DATA))
          .toInstance(RedisProducer.of(WEBHOOK_TRIGGER_EVENT_DATA, redisConfig,
              WEBHOOK_TRIGGER_EVENT_DATA_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
    }
  }
}