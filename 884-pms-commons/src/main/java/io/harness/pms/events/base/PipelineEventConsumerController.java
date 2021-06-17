package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineEventConsumerController implements Managed {
  private ExecutorService executorService =
      Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("pipeline-event-consumer-%d").build());
  private List<PmsAbstractRedisConsumer> abstractRedisConsumers = new ArrayList<>();

  public void register(PmsAbstractRedisConsumer consumer, int threads) {
    IntStream.rangeClosed(1, threads).forEach(value -> {
      abstractRedisConsumers.add(consumer);
      executorService.submit(consumer);
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    abstractRedisConsumers.forEach(PmsAbstractRedisConsumer::shutDown);
    executorService.shutdownNow();
    executorService.awaitTermination(1, TimeUnit.HOURS);
  }
}