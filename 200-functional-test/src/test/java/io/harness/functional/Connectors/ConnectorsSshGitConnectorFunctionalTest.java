package io.harness.functional.Connectors;

import static io.harness.generator.SettingGenerator.Settings.SSH_GIT_CONNECTOR_WITH_7999_PORT;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.SettingsUtils;

import software.wings.beans.SettingAttribute;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorsSshGitConnectorFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Test
  @Owner(developers = ARVIND)
  @Category(FunctionalTests.class)
  public void testSshConnectorWith7999Port() {
    OwnerManager.Owners owners = ownerManager.create();
    SettingAttribute connector = settingGenerator.ensurePredefined(seed, owners, SSH_GIT_CONNECTOR_WITH_7999_PORT);
    Boolean isValid = SettingsUtils.validateConnectivity(bearerToken, getAccount().getUuid(), connector)
                          .getJsonObject("resource.valid");
    assertThat(isValid).isTrue();
  }
}