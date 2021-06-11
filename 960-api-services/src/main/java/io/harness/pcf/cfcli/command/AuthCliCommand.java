package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.Flag;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Option;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
public class AuthCliCommand extends CfCliCommand {
  @Builder
  AuthCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      AuthOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.AUTH, arguments, options);
  }

  @Value
  @Builder
  public static class AuthOptions implements Options {
    @Option(value = "--origin") String origin;
    @Flag(value = "--client-credentials") boolean clientCredentials;
  }
}