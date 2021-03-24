package io.harness.pipeline.remote;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

@TargetModule(HarnessModule._888_PMS_CLIENT)
@OwnedBy(PIPELINE)
public class PipelineServiceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<PipelineServiceClient> {
  public PipelineServiceHttpClientFactory(ServiceHttpClientConfig config, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(config, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public PipelineServiceClient get() {
    return getRetrofit().create(PipelineServiceClient.class);
  }
}