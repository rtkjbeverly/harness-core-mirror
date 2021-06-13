package io.harness.exception;

import static io.harness.eraro.ErrorCode.AUTHENTICATION_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class AuthenticationException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public AuthenticationException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, AUTHENTICATION_ERROR, Level.ERROR, reportTargets, EnumSet.of(FailureType.AUTHENTICATION));
    param(MESSAGE_KEY, message);
  }

  public AuthenticationException(String message, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message, null, AUTHENTICATION_ERROR, level, reportTargets, EnumSet.of(FailureType.AUTHENTICATION));
    param(MESSAGE_KEY, message);
  }
}