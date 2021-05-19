package software.wings.helpers.ext.k8s.request;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.beans.GitConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sBlueGreenDeployTaskParametersTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithSingleDelegateSelector() {
    K8sBlueGreenDeployTaskParameters parameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .k8sClusterConfig(K8sClusterConfig.builder().build())
            .k8sDelegateManifestConfig(
                K8sDelegateManifestConfig.builder()
                    .gitConfig(GitConfig.builder().delegateSelectors(Collections.singletonList("primary")).build())
                    .build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithMultipleDelegateSelectors() {
    K8sBlueGreenDeployTaskParameters parameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .k8sClusterConfig(K8sClusterConfig.builder().build())
            .k8sDelegateManifestConfig(
                K8sDelegateManifestConfig.builder()
                    .gitConfig(GitConfig.builder().delegateSelectors(Arrays.asList("primary", "delegate")).build())
                    .build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.SELECTORS);
  }

  @Test
  @Owner(developers = OwnerRule.PARDHA)
  @Category(UnitTests.class)
  public void testFetchRequiredExecutionCapabilitiesWithoutDelegateSelectors() {
    K8sBlueGreenDeployTaskParameters parameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .k8sClusterConfig(K8sClusterConfig.builder().build())
            .k8sDelegateManifestConfig(
                K8sDelegateManifestConfig.builder().gitConfig(GitConfig.builder().build()).build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP);
  }
}