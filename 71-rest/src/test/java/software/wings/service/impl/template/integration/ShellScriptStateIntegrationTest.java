package software.wings.service.impl.template.integration;

import static io.harness.rule.OwnerRule.AADITI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateService;

import java.util.Arrays;
import java.util.UUID;

@Integration
@SetupScheduler
public class ShellScriptStateIntegrationTest extends WingsBaseTest {
  @Inject private TemplateFolderService templateFolderService;
  @Inject private TemplateService templateService;

  @Test(expected = WingsException.class)
  @Owner(developers = AADITI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateUpdateDeleteShellScriptTemplate() {
    // Create template
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType("BASH")
                                                  .scriptString("echo ${var}\n export A=\"aaa\"\n export B=\"bbb\"\n")
                                                  .outputVars("A,B")
                                                  .build();

    Template template = Template.builder()
                            .templateObject(shellScriptTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Integration State - Sample Script" + UUID.randomUUID().toString())
                            .variables(Arrays.asList(aVariable().type(TEXT).name("var").mandatory(true).build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables()).extracting("name").contains("var");
    ShellScriptTemplate savedShellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedShellScriptTemplate).isNotNull();
    assertThat(savedShellScriptTemplate.getScriptString()).isNotEmpty();

    // Update template
    ShellScriptTemplate anotherShellScriptTemplate =
        ShellScriptTemplate.builder().scriptString("echo ${var1}\n export A=\"aaa\"\n export B=\"bbb\"\n").build();
    savedTemplate.setTemplateObject(anotherShellScriptTemplate);
    savedTemplate.setVariables(Arrays.asList(aVariable().type(TEXT).name("var1").mandatory(true).build()));
    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getKeywords()).isNotEmpty();
    assertThat(updatedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(updatedTemplate.getVersion()).isEqualTo(2);
    assertThat(updatedTemplate.getVariables()).isNotEmpty();
    assertThat(updatedTemplate.getVariables()).extracting("name").contains("var1");
    savedShellScriptTemplate = (ShellScriptTemplate) savedTemplate.getTemplateObject();
    assertThat(savedShellScriptTemplate).isNotNull();
    assertThat(savedShellScriptTemplate.getScriptString()).isNotEmpty();

    // Delete template
    templateService.delete(GLOBAL_ACCOUNT_ID, savedTemplate.getUuid());

    // assert template deleted
    templateService.get(savedTemplate.getUuid());
  }
}
