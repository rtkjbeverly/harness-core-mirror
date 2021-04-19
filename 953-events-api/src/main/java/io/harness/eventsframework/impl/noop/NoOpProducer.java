package io.harness.eventsframework.impl.noop;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.AbstractProducer;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.eventsframework.producer.Message;

@OwnedBy(PL)
public class NoOpProducer extends AbstractProducer {
  public NoOpProducer(String topicName) {
    super(topicName, "dummyProducer");
  }

  @Override
  public String send(Message message) throws ProducerShutdownException {
    return "dummy-message-id";
  }

  @Override
  public void shutdown() {
    // Nothing required to shutdown
  }

  public static NoOpProducer of(String topicName) {
    return new NoOpProducer(topicName);
  }
}