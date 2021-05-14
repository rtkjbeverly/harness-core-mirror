package io.harness.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static io.github.resilience4j.core.IntervalFunction.ofExponentialRandomBackoff;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.exception.ExceptionUtils.indexOfType;

import io.harness.annotations.dev.OwnedBy;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(CDP)
public class RetryHelper {
  private static final int BACKOFF_INTERVAL_MINUTES = 1;
  private static final int MAX_ATTEMPTS = 3;
  private static final RetryRegistry globalRegistry = RetryRegistry.ofDefaults();

  static {
    globalRegistry.getEventPublisher()
        .onEntryAdded(entryAddedEvent -> {
          Retry addedRetry = entryAddedEvent.getAddedEntry();
          log.info("Retry {} added", addedRetry.getName());
        })
        .onEntryRemoved(entryRemovedEvent -> {
          Retry removedRetry = entryRemovedEvent.getRemovedEntry();
          log.info("Retry {} removed", removedRetry.getName());
        });
  }

  public static Retry getExponentialRetry(String name, Class[] retryExceptions) {
    final RetryConfig config =
        RetryConfig.custom()
            .maxAttempts(MAX_ATTEMPTS)
            .intervalFunction(ofExponentialRandomBackoff(Duration.ofMinutes(BACKOFF_INTERVAL_MINUTES).toMillis(), 2d))
            .retryOnException(t -> stream(retryExceptions).anyMatch(exception -> indexOfType(t, exception) != -1))
            .build();
    return globalRegistry.retry(name, config);
  }

  public static void registerEventListeners(Retry retry) {
    retry.getEventPublisher()
        .onRetry(event -> log.warn(event.toString()))
        .onError(event -> log.error(event.toString()))
        .onSuccess(event -> log.info(event.toString()));
  }
}