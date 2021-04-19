package io.harness.project.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class ProjectHttpClientFactory extends AbstractHttpClientFactory implements Provider<ProjectClient> {
  public ProjectHttpClientFactory(ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(projectManagerClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public ProjectClient get() {
    return getRetrofit().create(ProjectClient.class);
  }
}