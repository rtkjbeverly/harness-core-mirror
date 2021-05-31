package io.harness.serializer.spring.converters.consumerconfig;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ConsumerConfig;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class ConsumerConfigReadConverter extends ProtoReadConverter<ConsumerConfig> {
  public ConsumerConfigReadConverter() {
    super(ConsumerConfig.class);
  }
}