package software.wings.beans.command;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecCommandUnitAbstractYaml extends SshCommandUnit.Yaml {
  // maps to commandPath
  private String workingDirectory;
  private String scriptType;
  // maps to commandString
  private String command;
  // maps to tailPatterns
  private List<TailFilePatternEntry.Yaml> filePatternEntryList;

  public ExecCommandUnitAbstractYaml(String commandUnitType) {
    super(commandUnitType);
  }

  public ExecCommandUnitAbstractYaml(String name, String commandUnitType, String deploymentType,
      String workingDirectory, String scriptType, String command,
      List<TailFilePatternEntry.Yaml> filePatternEntryList) {
    super(name, commandUnitType, deploymentType);
    this.workingDirectory = workingDirectory;
    this.scriptType = scriptType;
    this.command = command;
    this.filePatternEntryList = filePatternEntryList;
  }
}