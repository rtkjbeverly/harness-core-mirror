package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.Flag;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
public class LogsCliCommand extends CfCliCommand {
  @Builder
  LogsCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      LogsOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.LOGS, arguments, options);
  }

  @Value
  @Builder
  public static class LogsOptions implements Options {
    @Flag(value = "--recent") boolean recent;
  }
}