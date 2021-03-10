package software.wings.beans.artifact;

import static io.harness.rule.OwnerRule.AGORODETKI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactStreamAttributesTest {
  private static final String IMAGE_NAME = "projectName/imageName";

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldPreProcessGcrUrlToValidate() {
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder()
                                                            .registryHostName("gcr.io")
                                                            .imageName(IMAGE_NAME)
                                                            .enhancedGcrConnectivityCheckEnabled(true)
                                                            .build();
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            "https://gcr.io/v2/projectName/imageName/tags/list", null);
    List<ExecutionCapability> executionCapabilities = artifactStreamAttributes.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).containsOnly(httpConnectionExecutionCapability);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldAddCapabilityCheckForHostNameStrictly() {
    ArtifactStreamAttributes artifactStreamAttributes = software.wings.beans.artifact.ArtifactStreamAttributes.builder()
                                                            .registryHostName("nonGcrHost.com")
                                                            .imageName(IMAGE_NAME)
                                                            .enhancedGcrConnectivityCheckEnabled(true)
                                                            .build();
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            "https://nonGcrHost.com/", null);
    List<ExecutionCapability> executionCapabilities = artifactStreamAttributes.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).containsOnly(httpConnectionExecutionCapability);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldAddCapabilityCheckForHostNameStrictlyWhenFFisDisabled() {
    ArtifactStreamAttributes artifactStreamAttributes = software.wings.beans.artifact.ArtifactStreamAttributes.builder()
                                                            .registryHostName("gcr.io")
                                                            .imageName(IMAGE_NAME)
                                                            .enhancedGcrConnectivityCheckEnabled(false)
                                                            .build();
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability("https://gcr.io/", null);
    List<ExecutionCapability> executionCapabilities = artifactStreamAttributes.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).containsOnly(httpConnectionExecutionCapability);
  }
}