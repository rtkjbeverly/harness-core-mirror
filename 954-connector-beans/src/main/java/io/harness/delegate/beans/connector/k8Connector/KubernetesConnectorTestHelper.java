package io.harness.delegate.beans.connector.k8Connector;

import static io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType.SERVICE_ACCOUNT;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;

import io.harness.encryption.SecretRefData;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KubernetesConnectorTestHelper {
  public KubernetesClusterConfigDTO inClusterDelegateK8sConfig() {
    KubernetesCredentialDTO kubernetesCredentialDTO =
        KubernetesCredentialDTO.builder().config(null).kubernetesCredentialType(INHERIT_FROM_DELEGATE).build();
    return createKubernetesClusterConfig(kubernetesCredentialDTO);
  }

  public KubernetesClusterConfigDTO manualK8sConfig() {
    final KubernetesCredentialDTO k8sCredentials = KubernetesCredentialDTO.builder()
                                                       .kubernetesCredentialType(MANUAL_CREDENTIALS)
                                                       .config(KubernetesClusterDetailsDTO.builder()
                                                                   .masterUrl("http://localhost")
                                                                   .auth(createServiceAccountDTO())
                                                                   .build())
                                                       .build();
    return createKubernetesClusterConfig(k8sCredentials);
  }

  private static KubernetesAuthDTO createServiceAccountDTO() {
    KubernetesServiceAccountDTO kubernetesServiceAccountDTO =
        KubernetesServiceAccountDTO.builder().serviceAccountTokenRef(new SecretRefData("sdhbjbvjr")).build();
    return KubernetesAuthDTO.builder().authType(SERVICE_ACCOUNT).credentials(kubernetesServiceAccountDTO).build();
  }

  private static KubernetesClusterConfigDTO createKubernetesClusterConfig(KubernetesCredentialDTO k8sCredentials) {
    return KubernetesClusterConfigDTO.builder().credential(k8sCredentials).build();
  }
}