package io.harness.engine.pms.commons.events;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.DUMMY_REDIS_URL;
import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.OrchestrationModuleConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.pms.contracts.plan.ConsumerConfig.ConfigCase;
import io.harness.pms.contracts.plan.Redis;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.PmsSdkInstance;
import io.harness.pms.sdk.PmsSdkInstance.PmsSdkInstanceKeys;
import io.harness.redis.RedisConfig;
import io.harness.utils.RetryUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PmsEventSender {
  private static final RetryPolicy<Object> retryPolicy = RetryUtils.getRetryPolicy("Error Getting Producer..Retrying",
      "Failed to obtain producer", Collections.singletonList(ExecutionException.class), Duration.ofMillis(10), 3, log);
  @Inject private MongoTemplate mongoTemplate;
  @Inject private OrchestrationModuleConfig moduleConfig;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;

  private final LoadingCache<ProducerCacheKey, Producer> producerCache =
      CacheBuilder.newBuilder()
          .maximumSize(100)
          .expireAfterAccess(300, TimeUnit.MINUTES)
          .build(new CacheLoader<ProducerCacheKey, Producer>() {
            @Override
            public Producer load(ProducerCacheKey cacheKey) {
              return obtainProducer(cacheKey);
            }
          });

  public String sendEvent(
      ByteString eventData, PmsEventCategory eventCategory, String serviceName, String accountId, boolean isMonitored) {
    log.info("Sending {} event for {} to the producer", eventCategory, serviceName);
    Producer producer = obtainProducer(eventCategory, serviceName);

    ImmutableMap.Builder<String, String> metadataBuilder =
        ImmutableMap.<String, String>builder().put(SERVICE_NAME, serviceName);
    if (isMonitored && pmsFeatureFlagService.isEnabled(accountId, FeatureName.PIPELINE_MONITORING)) {
      metadataBuilder.put(PIPELINE_MONITORING_ENABLED, "true");
    }

    String messageId =
        producer.send(Message.newBuilder().putAllMetadata(metadataBuilder.build()).setData(eventData).build());
    log.info("Successfully Sent {} event for {} to the producer. MessageId {}", eventCategory, serviceName, messageId);
    return messageId;
  }

  @VisibleForTesting
  Producer obtainProducer(PmsEventCategory eventCategory, String serviceName) {
    Producer producer =
        Failsafe.with(retryPolicy)
            .get(()
                     -> producerCache.get(
                         ProducerCacheKey.builder().eventCategory(eventCategory).serviceName(serviceName).build()));
    if (producer == null) {
      throw new RuntimeException("Cannot create Event Framework producer");
    }
    return producer;
  }

  @VisibleForTesting
  Producer obtainProducer(ProducerCacheKey cacheKey) {
    PmsSdkInstance instance = getPmsSdkInstance(cacheKey.getServiceName());
    switch (cacheKey.getEventCategory()) {
      case INTERRUPT_EVENT:
        return extractProducer(
            instance.getInterruptConsumerConfig(), EventsFrameworkConstants.PIPELINE_INTERRUPT_EVENT_MAX_TOPIC_SIZE);
      case ORCHESTRATION_EVENT:
        return extractProducer(instance.getOrchestrationEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_ORCHESTRATION_EVENT_MAX_TOPIC_SIZE);
      case FACILITATOR_EVENT:
        return extractProducer(instance.getFacilitatorEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_FACILITATOR_EVENT_MAX_TOPIC_SIZE);
      case NODE_START:
        return extractProducer(instance.getNodeStartEventConsumerConfig(),
            EventsFrameworkConstants.PIPELINE_NODE_START_EVENT_MAX_TOPIC_SIZE);
      case PROGRESS_EVENT:
        return extractProducer(
            instance.getProgressEventConsumerConfig(), EventsFrameworkConstants.PIPELINE_PROGRESS_MAX_TOPIC_SIZE);
      case NODE_ADVISE:
        return extractProducer(
            instance.getNodeAdviseEventConsumerConfig(), EventsFrameworkConstants.PIPELINE_NODE_ADVISE_MAX_TOPIC_SIZE);
      case NODE_RESUME:
        return extractProducer(
            instance.getNodeResumeEventConsumerConfig(), EventsFrameworkConstants.PIPELINE_NODE_RESUME_MAX_TOPIC_SIZE);
      default:
        throw new InvalidRequestException("Invalid Event Category while obtaining Producer");
    }
  }

  private Producer extractProducer(ConsumerConfig consumerConfig, int topicSize) {
    ConfigCase configCase = consumerConfig.getConfigCase();
    switch (configCase) {
      case REDIS:
        Redis redis = consumerConfig.getRedis();
        return buildRedisProducer(redis.getTopicName(), moduleConfig.getEventsFrameworkConfiguration().getRedisConfig(),
            PIPELINE_SERVICE.getServiceId(), topicSize);
      case CONFIG_NOT_SET:
      default:
        throw new InvalidRequestException("No producer found for Config Case " + configCase.name());
    }
  }

  private Producer buildRedisProducer(String topicName, RedisConfig redisConfig, String serviceId, int topicSize) {
    return redisConfig.getRedisUrl().equals(DUMMY_REDIS_URL)
        ? NoOpProducer.of(topicName)
        : RedisProducer.of(topicName, redisConfig, topicSize, serviceId);
  }

  PmsSdkInstance getPmsSdkInstance(String serviceName) {
    Query query = query(where(PmsSdkInstanceKeys.name).is(serviceName));
    PmsSdkInstance instance = mongoTemplate.findOne(query, PmsSdkInstance.class);
    if (instance == null) {
      throw new InvalidRequestException("Sdk Not registered for Service name" + serviceName);
    }
    return instance;
  }
}