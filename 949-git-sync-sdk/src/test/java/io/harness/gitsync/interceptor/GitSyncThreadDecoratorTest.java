package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(DX)
public class GitSyncThreadDecoratorTest {
  GitSyncThreadDecorator gitSyncThreadDecorator = new GitSyncThreadDecorator();

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testDecorate() {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    queryParameters.add("branch", "branch");
    final String branch = gitSyncThreadDecorator.getRequestParamFromContext("branch", null, queryParameters);
    assertThat(branch).isEqualTo("branch");

    MultivaluedMap<String, String> queryParameters_1 = new MultivaluedHashMap<>();

    queryParameters_1.add("branch", "5%2F10");
    final String branch_1 = gitSyncThreadDecorator.getRequestParamFromContext("branch", null, queryParameters_1);
    assertThat(branch_1).isEqualTo("5/10");
  }
}