package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(PL)
public class TaskSetupAbstractionHelperTest extends CategoryTest {
  @Mock private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private TaskSetupAbstractionHelper taskSetupAbstractionHelper;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static final String projectIdentifier = "PROJECT_IDENTIFIER";
  private static final String orgIdentifier = "ORG_IDENTIFIER";
  private static final String accountId = "ACCOUNT_ID";

  private static final String projectOwner = "ORG_IDENTIFIER/PROJECT_IDENTIFIER";
  private static final String orgOwner = "ORG_IDENTIFIER";

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void getOwnerTest() {
    validateOwner(true, orgOwner, projectOwner);
    validateOwner(false, null, null);
  }

  private void validateOwner(boolean featureFlagEnabled, String orgOwner, String projectOwner) {
    doReturn(featureFlagEnabled).when(featureFlagService).isEnabled(FeatureName.DELEGATE_OWNERS, accountId);

    EncryptionConfig encryptionConfig = Mockito.mock(EncryptionConfig.class);
    // FF: <input>; Account level
    doReturn(accountId).when(encryptionConfig).getAccountId();
    assertThat(taskSetupAbstractionHelper.getOwner(accountId, null, null)).isNull();

    // FF: <input>; Org level
    assertThat(taskSetupAbstractionHelper.getOwner(accountId, orgIdentifier, null)).isEqualTo(orgOwner);

    // FF: <input>; Project level
    assertThat(taskSetupAbstractionHelper.getOwner(accountId, orgIdentifier, projectIdentifier))
        .isEqualTo(projectOwner);

    // FF: <input>; Org missing
    assertThat(taskSetupAbstractionHelper.getOwner(accountId, null, projectIdentifier)).isNull();
  }
}