package io.harness.exception;

import static io.harness.eraro.ErrorCode.CONNECTION_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class ConnectException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public ConnectException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, CONNECTION_ERROR, Level.ERROR, reportTargets, EnumSet.of(FailureType.CONNECTIVITY));
    param(MESSAGE_KEY, message);
  }

  public ConnectException(String message, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, CONNECTION_ERROR, level, reportTargets, EnumSet.of(FailureType.CONNECTIVITY));
    param(MESSAGE_KEY, message);
  }
}