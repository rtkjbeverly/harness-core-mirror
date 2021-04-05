package io.harness.terraform.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(CDP)
public class TerraformExecuteStepRequest {
  File tfBackendConfigsFile;
  File tfOutputsFile;
  List<String> tfVarFilePaths;
  String varParams; // Needed to send inline variable values in CG
  @Nonnull @NotEmpty String scriptDirectory;
  Map<String, String> envVars;
  List<String> targets;
  String workspace;
  boolean isRunPlanOnly;
  EncryptedRecordData encryptedTfPlan;
  EncryptionConfig encryptionConfig;
  boolean isSkipRefreshBeforeApplyingPlan;
  boolean isSaveTerraformJson;
  @Nonnull LogCallback logCallback;
  @Nonnull PlanJsonLogOutputStream planJsonLogOutputStream;
}