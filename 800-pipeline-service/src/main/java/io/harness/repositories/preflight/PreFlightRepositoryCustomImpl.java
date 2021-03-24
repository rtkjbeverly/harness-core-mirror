package io.harness.repositories.preflight;

import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.entity.PreFlightEntity.PreFlightEntityKeys;

import com.google.inject.Inject;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class PreFlightRepositoryCustomImpl implements PreFlightRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public PreFlightEntity update(Criteria criteria, PreFlightEntity entity) {
    Query query = new Query(criteria);
    Update update = new Update();
    update.set(PreFlightEntityKeys.connectorCheckResponse, entity.getConnectorCheckResponse());
    update.set(PreFlightEntityKeys.pipelineInputResponse, entity.getPipelineInputResponse());
    RetryPolicy<Object> retryPolicy = getRetryPolicy();
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), PreFlightEntity.class));
  }

  private RetryPolicy<Object> getRetryPolicy() {
    Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
    int MAX_ATTEMPTS = 3;
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event
            -> log.info(
                "[Retrying]: Failed updating Service; attempt: {}", event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event
            -> log.error(
                "[Failed]: Failed updating Service; attempt: {}", event.getAttemptCount(), event.getFailure()));
  }
}