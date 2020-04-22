package io.harness.commandlibrary.server.common;

import lombok.experimental.UtilityClass;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO.EnrichedCommandVersionDTOBuilder;
import software.wings.beans.commandlibrary.CommandVersionEntity;

@UtilityClass
public class CommandVersionUtils {
  public static EnrichedCommandVersionDTOBuilder populateCommandVersionDTO(
      EnrichedCommandVersionDTOBuilder builder, CommandVersionEntity commandVersionEntity) {
    if (commandVersionEntity != null) {
      builder.commandName(commandVersionEntity.getCommandName())
          .commandStoreName(commandVersionEntity.getCommandStoreName())
          .description(commandVersionEntity.getDescription())
          .version(commandVersionEntity.getVersion())
          .yamlContent(commandVersionEntity.getYamlContent())
          .templateObject(commandVersionEntity.getTemplateObject())
          .variables(commandVersionEntity.getVariables());
    }
    return builder;
  }
}
