package io.harness.ci.serializer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.beans.yaml.extended.reports.JUnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReportType;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.Report;
import io.harness.product.ci.engine.proto.RunTestsStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;

public class RunTestsStepProtobufSerializer implements ProtobufStepSerializer<RunTestsStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId, String logKey) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    RunTestsStepInfo runTestsStepInfo = (RunTestsStepInfo) ciStepInfo;

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout(), ciStepInfo.getDefaultTimeout());

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    RunTestsStep.Builder runTestsStepBuilder = RunTestsStep.newBuilder();

    String preTestCommand = RunTimeInputHandler.resolveStringParameter(
        "Command", "RunTests", step.getIdentifier(), runTestsStepInfo.getPreCommand(), false);
    if (StringUtils.isNotEmpty(preTestCommand)) {
      runTestsStepBuilder.setPreTestCommand(preTestCommand);
    }
    String postTestCommand = RunTimeInputHandler.resolveStringParameter(
        "Command", "RunTests", step.getIdentifier(), runTestsStepInfo.getPostCommand(), false);
    if (StringUtils.isNotEmpty(postTestCommand)) {
      runTestsStepBuilder.setPostTestCommand(postTestCommand);
    }
    runTestsStepBuilder.setArgs(runTestsStepInfo.getArgs());
    runTestsStepBuilder.setContainerPort(port);
    runTestsStepBuilder.setLanguage(runTestsStepInfo.getLanguage());
    runTestsStepBuilder.setBuildTool(runTestsStepInfo.getBuildTool());
    runTestsStepBuilder.setRunOnlySelectedTests(runTestsStepInfo.isRunOnlySelectedTests());
    runTestsStepBuilder.setPackages(runTestsStepInfo.getPackages());

    List<String> output = RunTimeInputHandler.resolveListParameter(
        "OutputVariables", "RunTests", step.getIdentifier(), runTestsStepInfo.getOutputVariables(), false);
    if (isNotEmpty(output)) {
      runTestsStepBuilder.addAllEnvVarOutputs(output);
    }

    if (runTestsStepInfo.getTestAnnotations() != null) {
      runTestsStepBuilder.setTestAnnotations(runTestsStepInfo.getTestAnnotations());
    }

    UnitTestReport reports = runTestsStepInfo.getReports();
    if (reports != null) {
      if (reports.getType() == UnitTestReportType.JUNIT) {
        JUnitTestReport junitTestReport = (JUnitTestReport) reports.getSpec();
        List<String> resolvedReport = junitTestReport.resolve(step.getIdentifier(), "runtests");
        Report report = Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolvedReport).build();
        runTestsStepBuilder.addReports(report);
      }
    }

    runTestsStepBuilder.setContext(
        StepContext.newBuilder().setNumRetries(runTestsStepInfo.getRetry()).setExecutionTimeoutSecs(timeout).build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runTestsStepInfo.getDisplayName()).orElse(""))
        .setRunTests(runTestsStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setLogKey(logKey)
        .build();
  }

  public UnitStep serializeStepWithStepParameters(RunTestsStepInfo runTestsStepInfo, Integer port, String callbackId,
      String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout, String accountId,
      String stepName) {
    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    RunTestsStep.Builder runTestsStepBuilder = RunTestsStep.newBuilder();

    String preTestCommand = RunTimeInputHandler.resolveStringParameter(
        "Command", "RunTests", identifier, runTestsStepInfo.getPreCommand(), false);
    if (StringUtils.isNotEmpty(preTestCommand)) {
      runTestsStepBuilder.setPreTestCommand(preTestCommand);
    }
    String postTestCommand = RunTimeInputHandler.resolveStringParameter(
        "Command", "RunTests", identifier, runTestsStepInfo.getPostCommand(), false);
    if (StringUtils.isNotEmpty(postTestCommand)) {
      runTestsStepBuilder.setPostTestCommand(postTestCommand);
    }
    runTestsStepBuilder.setArgs(runTestsStepInfo.getArgs());
    runTestsStepBuilder.setContainerPort(port);
    runTestsStepBuilder.setLanguage(runTestsStepInfo.getLanguage());
    runTestsStepBuilder.setBuildTool(runTestsStepInfo.getBuildTool());
    runTestsStepBuilder.setRunOnlySelectedTests(runTestsStepInfo.isRunOnlySelectedTests());
    runTestsStepBuilder.setPackages(runTestsStepInfo.getPackages());

    List<String> output = RunTimeInputHandler.resolveListParameter(
        "OutputVariables", "RunTests", identifier, runTestsStepInfo.getOutputVariables(), false);
    if (isNotEmpty(output)) {
      runTestsStepBuilder.addAllEnvVarOutputs(output);
    }

    if (runTestsStepInfo.getTestAnnotations() != null) {
      runTestsStepBuilder.setTestAnnotations(runTestsStepInfo.getTestAnnotations());
    }

    UnitTestReport reports = runTestsStepInfo.getReports();
    if (reports != null) {
      if (reports.getType() == UnitTestReportType.JUNIT) {
        JUnitTestReport junitTestReport = (JUnitTestReport) reports.getSpec();
        List<String> resolvedReport = junitTestReport.resolve(identifier, "runtests");
        Report report = Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolvedReport).build();
        runTestsStepBuilder.addReports(report);
      }
    }
    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, runTestsStepInfo.getDefaultTimeout());

    runTestsStepBuilder.setContext(StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build());

    return UnitStep.newBuilder()
        .setId(identifier)
        .setAccountId(accountId)
        .setContainerPort(port)
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setRunTests(runTestsStepBuilder.build())
        .setLogKey(logKey)
        .build();
  }
}