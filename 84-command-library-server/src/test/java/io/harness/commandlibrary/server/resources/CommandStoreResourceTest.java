package io.harness.commandlibrary.server.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Variable.VariableBuilder.aVariable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.commandlibrary.server.CommandLibraryServerBaseTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;

import java.util.Collection;
import java.util.List;

public class CommandStoreResourceTest extends CommandLibraryServerBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject CommandStoreResource commandStoreResource = new CommandStoreResource();

  private final Multimap<String, String> commandMap = HashMultimap.create();
  @Before
  public void setUp() throws Exception {
    createCommands();
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getCommandStores() {
    final RestResponse<List<CommandStoreDTO>> commandStoresResponse = commandStoreResource.getCommandStores("account");
    final List<CommandStoreDTO> commandStores = commandStoresResponse.getResource();
    assertThat(commandStores).hasSize(1);
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getCommandCategories() {
    final RestResponse<List<String>> commandCategories =
        commandStoreResource.getCommandCategories("accountid", "harness");
    assertThat(commandCategories.getResource()).contains("Azure");
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_listCommands() {
    final RestResponse<PageResponse<CommandDTO>> restResponse = commandStoreResource.listCommands(
        "accountid", "harness", aPageRequest().withOffset("0").withLimit("10").build(), 1, "Azure");

    final PageResponse<CommandDTO> pageResponse = restResponse.getResource();
    assertThat(pageResponse.getTotal()).isEqualTo(2L);
    assertThat(commandMap.keySet()).contains(pageResponse.get(0).getName(), pageResponse.get(1).getName());
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getCommandDetails() {
    final String commandName = commandMap.keySet().iterator().next();
    final CommandDTO commandDTO =
        commandStoreResource.getCommandDetails("accountid", "harness", commandName).getResource();
    assertThat(commandDTO.getName()).isEqualTo(commandName);
    final Collection<String> versions = commandMap.get(commandName);
    assertThat(versions).contains(commandDTO.getLatestVersion().getVersion());
    assertThat(commandDTO.getCommandStoreName()).isEqualTo("harness");
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_saveCommand() {
    final CommandEntity commandEntity1 = CommandEntity.builder()
                                             .commandStoreName("harness")
                                             .type("HTTP")
                                             .name("Health Check 123")
                                             .description("This is http template for health check")
                                             .category("Azure")
                                             .imageUrl("https://app.harness.io/img/harness-logo.png")
                                             .latestVersion("2.5")
                                             .build();

    final CommandEntity savedCommandEntity =
        commandStoreResource.saveCommand("accountid", commandEntity1).getResource();
    assertThat(savedCommandEntity.getName()).isEqualTo(commandEntity1.getName());
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getVersionDetails() {
    final String commandId = commandMap.keySet().iterator().next();
    final String versionId = commandMap.get(commandId).iterator().next();
    final EnrichedCommandVersionDTO commandVersionDTO =
        commandStoreResource.getVersionDetails("accountid", "harness", commandId, versionId).getResource();
    assertThat(commandVersionDTO.getVersion()).isEqualTo(versionId);
    assertThat(commandVersionDTO.getCommandName()).isEqualTo(commandId);
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_saveCommandVersion() {
    final CommandVersionEntity commandVersionEntity1 =
        CommandVersionEntity.builder()
            .commandName("commandid")
            .commandStoreName("harness")
            .description("version description 1")
            .version("1.0")
            .yamlContent("yaml content 1")
            .templateObject(HttpTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var1").value("val1").build(), aVariable().name("var2").value("val2").build()))
            .build();

    final CommandVersionEntity savedVersion =
        commandStoreResource.saveCommandVersion("Accountid", commandVersionEntity1).getResource();
    assertThat(savedVersion.getVersion()).isEqualTo(commandVersionEntity1.getVersion());
    assertThat(savedVersion.getCommandName()).isEqualTo(commandVersionEntity1.getCommandName());
  }

  private void createCommands() {
    final CommandEntity commandEntity1 = CommandEntity.builder()
                                             .commandStoreName("harness")
                                             .type("HTTP")
                                             .name("Health Check")
                                             .description("This is http template for health check")
                                             .category("Azure")
                                             .imageUrl("https://app.harness.io/img/harness-logo.png")
                                             .latestVersion("1.5")
                                             .build();

    final CommandEntity commandEntity2 =
        CommandEntity.builder()
            .commandStoreName("harness")
            .type("SSH")
            .name("Stop")
            .description("This is a command to stop service by invoking scripts over SSH to the individual instances")
            .category("Azure")
            .imageUrl("https://app.harness.io/img/harness-logo.png")
            .latestVersion("2.1")
            .build();

    final String commandId1 = wingsPersistence.save(commandEntity1);

    final String commandId2 = wingsPersistence.save(commandEntity2);

    commandEntity1.setUuid(commandId1);

    commandEntity2.setUuid(commandId2);
    createCommand1Versions(commandEntity1);
    createCommand2Versions(commandEntity2);
  }

  private void createCommand1Versions(CommandEntity commandEntity) {
    final CommandVersionEntity commandVersionEntity1 =
        CommandVersionEntity.builder()
            .commandName(commandEntity.getName())
            .commandStoreName(commandEntity.getCommandStoreName())
            .description("version description 1")
            .version("1.0")
            .yamlContent("yaml content 1")
            .templateObject(HttpTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var1").value("val1").build(), aVariable().name("var2").value("val2").build()))
            .build();

    final CommandVersionEntity commandVersionEntity2 =
        CommandVersionEntity.builder()
            .commandName(commandEntity.getName())
            .commandStoreName(commandEntity.getCommandStoreName())
            .description("version description 2")
            .version("1.5")
            .yamlContent("yaml content 2")
            .templateObject(HttpTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var3").value("val3").build(), aVariable().name("var4").value("val4").build()))
            .build();

    final String version1Id = wingsPersistence.save(commandVersionEntity1);
    final String version2Id = wingsPersistence.save(commandVersionEntity2);

    commandVersionEntity1.setUuid(version1Id);
    commandVersionEntity2.setUuid(version2Id);
    commandMap.put(commandEntity.getName(), commandVersionEntity1.getVersion());
    commandMap.put(commandEntity.getName(), commandVersionEntity2.getVersion());
  }

  private void createCommand2Versions(CommandEntity commandEntity) {
    final CommandVersionEntity commandVersionEntity1 =
        CommandVersionEntity.builder()
            .commandName(commandEntity.getName())
            .commandStoreName(commandEntity.getCommandStoreName())
            .description("version description 3")
            .version("1.0")
            .yamlContent("yaml content 3")
            .templateObject(SshCommandTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var5").value("val1").build(), aVariable().name("var2").value("val2").build()))
            .build();

    final CommandVersionEntity commandVersionEntity2 =
        CommandVersionEntity.builder()
            .commandName(commandEntity.getName())
            .commandStoreName(commandEntity.getCommandStoreName())
            .description("version description 4")
            .version("1.5")
            .yamlContent("yaml content 4")
            .templateObject(SshCommandTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var6").value("val3").build(), aVariable().name("var4").value("val4").build()))
            .build();

    final CommandVersionEntity commandVersionEntity3 =
        CommandVersionEntity.builder()
            .commandName(commandEntity.getName())
            .commandStoreName(commandEntity.getCommandStoreName())
            .description("version description 5")
            .version("2.1")
            .yamlContent("yaml content 5")
            .templateObject(SshCommandTemplate.builder().build())
            .variables(ImmutableList.of(
                aVariable().name("var7").value("val3").build(), aVariable().name("var4").value("val4").build()))
            .build();

    final String version1Id = wingsPersistence.save(commandVersionEntity1);
    final String version2Id = wingsPersistence.save(commandVersionEntity2);
    final String version3Id = wingsPersistence.save(commandVersionEntity3);

    commandVersionEntity1.setUuid(version1Id);
    commandVersionEntity2.setUuid(version2Id);
    commandVersionEntity3.setUuid(version3Id);

    commandMap.put(commandEntity.getName(), commandVersionEntity1.getVersion());
    commandMap.put(commandEntity.getName(), commandVersionEntity2.getVersion());
    commandMap.put(commandEntity.getName(), commandVersionEntity3.getVersion());
  }
}