package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.NotifyResponse;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData.ContinuousVerificationExecutionMetaDataKeys;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractMetricAnalysisStateTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private AppService appService;
  @Inject protected MetricDataAnalysisService metricAnalysisService;
  @Inject protected CVActivityLogService cvActivityLogService;
  @Inject private KryoSerializer kryoSerializer;
  @Mock private ExecutionContext executionContext;
  private AppDynamicsState appDynamicsState = new AppDynamicsState(generateUuid());
  private String accountId;
  private String stateExecutionId;
  private String appId;

  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    FieldUtils.writeField(appDynamicsState, "featureFlagService", featureFlagService, true);
    FieldUtils.writeField(appDynamicsState, "continuousVerificationService", continuousVerificationService, true);
    FieldUtils.writeField(appDynamicsState, "wingsPersistence", wingsPersistence, true);
    FieldUtils.writeField(appDynamicsState, "appService", appService, true);
    FieldUtils.writeField(appDynamicsState, "metricAnalysisService", metricAnalysisService, true);
    FieldUtils.writeField(appDynamicsState, "cvActivityLogService", cvActivityLogService, true);
    accountId = wingsPersistence.save(anAccount().withAccountName(generateUuid()).build());
    appId = wingsPersistence.save(anApplication().name("Harness Verification").accountId(accountId).build());
    stateExecutionId = generateUuid();
    when(executionContext.getStateExecutionInstanceId()).thenReturn(stateExecutionId);
    wingsPersistence.save(StateExecutionInstance.Builder.aStateExecutionInstance()
                              .uuid(stateExecutionId)
                              .displayName("name")
                              .stateExecutionMap(new HashMap<String, StateExecutionData>(
                                  ImmutableMap.of("name", new VerificationStateAnalysisExecutionData())))
                              .build());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncErrorStatus() {
    for (ExecutionStatus executionStatus : ExecutionStatus.brokeStatuses()) {
      String errorMsg = generateUuid();
      Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(executionStatus, errorMsg);
      createMetaDataExecutionData(ExecutionStatus.RUNNING);
      ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData =
          wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
              .filter(ContinuousVerificationExecutionMetaDataKeys.stateExecutionId, stateExecutionId)
              .get();
      assertThat(continuousVerificationExecutionMetaData.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
      ExecutionResponse executionResponse =
          appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
      assertThat(executionResponse.getExecutionStatus()).isEqualTo(executionStatus);
      assertThat(executionResponse.getErrorMessage()).isEqualTo(errorMsg);
      assertThat(executionResponse.getStateExecutionData())
          .isEqualTo(((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
                         .getStateExecutionData());
      continuousVerificationExecutionMetaData =
          wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
              .filter(ContinuousVerificationExecutionMetaDataKeys.stateExecutionId, stateExecutionId)
              .get();
      assertThat(continuousVerificationExecutionMetaData.getExecutionStatus()).isEqualTo(executionStatus);
      continuousVerificationExecutionMetaData.setExecutionStatus(ExecutionStatus.RUNNING);
      wingsPersistence.save(continuousVerificationExecutionMetaData);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncNoAnalysisQA() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    enableFeatureFlag(FeatureName.CV_SUCCEED_FOR_ANOMALY);
    saveAnalysisContext(dataAnalysisResponse);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    ((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
        .getStateExecutionData()
        .setAnalysisMinute(0);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("No Analysis result found. This is not a failure.");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncQANotFailWithAnomaly() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    enableFeatureFlag(FeatureName.CV_SUCCEED_FOR_ANOMALY);
    saveAnalysisContext(dataAnalysisResponse);
    saveMetricAnalysisRecord(RiskLevel.HIGH);

    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncFailWithAnomaly() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse);
    saveMetricAnalysisRecord(RiskLevel.HIGH);

    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_LowRiskWithLowTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 1);
    saveMetricAnalysisRecord(RiskLevel.LOW);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_LowAndNARiskWithLowTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 1);
    saveMetricAnalysisRecord(RiskLevel.LOW, RiskLevel.NA);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_LowRiskWithMediumTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 2);
    saveMetricAnalysisRecord(RiskLevel.LOW);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_LowRiskWithHighTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 3);
    saveMetricAnalysisRecord(RiskLevel.LOW);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_MediumRiskWithLowTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 1);
    saveMetricAnalysisRecord(RiskLevel.MEDIUM);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_MediumRiskWithMediumTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 2);
    saveMetricAnalysisRecord(RiskLevel.MEDIUM);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_MediumRiskWithHighTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 3);
    saveMetricAnalysisRecord(RiskLevel.MEDIUM);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_HighRiskWithLowTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 1);
    saveMetricAnalysisRecord(RiskLevel.HIGH);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_HighRiskWithMediumTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 2);
    saveMetricAnalysisRecord(RiskLevel.HIGH);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_HighRiskWithHighTolerance() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse, 3);
    saveMetricAnalysisRecord(RiskLevel.HIGH);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);

    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncNoMetricsQA() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    enableFeatureFlag(FeatureName.CV_SUCCEED_FOR_ANOMALY);
    saveAnalysisContext(dataAnalysisResponse);
    wingsPersistence.save(NewRelicMetricAnalysisRecord.builder()
                              .analysisMinute(5)
                              .appId(appId)
                              .stateExecutionId(stateExecutionId)
                              .build());
    ((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
        .getStateExecutionData()
        .setAnalysisMinute(0);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandleAsyncManualAction() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    ContinuousVerificationExecutionMetaData metadata = createMetaDataExecutionData(ExecutionStatus.RUNNING);
    metadata.setExecutionStatus(ExecutionStatus.SUCCESS);
    metadata.setManualOverride(true);
    wingsPersistence.save(metadata);

    enableFeatureFlag(FeatureName.CV_SUCCEED_FOR_ANOMALY);
    saveAnalysisContext(dataAnalysisResponse);
    wingsPersistence.save(NewRelicMetricAnalysisRecord.builder()
                              .analysisMinute(5)
                              .appId(appId)
                              .stateExecutionId(stateExecutionId)
                              .build());
    ((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
        .getStateExecutionData()
        .setAnalysisMinute(0);
    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncV2QANotFailWithAnomaly() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    enableFeatureFlag(FeatureName.CV_SUCCEED_FOR_ANOMALY);
    saveAnalysisContext(dataAnalysisResponse);
    saveMetricAnalysisRecord(RiskLevel.HIGH);

    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncV2FailWithAnomaly() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse);
    saveMetricAnalysisRecord(RiskLevel.HIGH);

    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandleAsyncV2NoFailWithoutAnomaly() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.SUCCESS, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse);
    wingsPersistence.save(
        NewRelicMetricAnalysisRecord.builder()
            .analysisMinute(5)
            .appId(appId)
            .stateExecutionId(stateExecutionId)
            .metricAnalyses(Lists.newArrayList(
                NewRelicMetricAnalysis.builder().metricName(generateUuid()).riskLevel(RiskLevel.MEDIUM).build(),
                NewRelicMetricAnalysis.builder().metricName(generateUuid()).riskLevel(RiskLevel.LOW).build()))
            .build());

    ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(executionContext, dataAnalysisResponse);
    validateExecutionResponse(dataAnalysisResponse, executionResponse, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNotifyStateHandleAsyncNoVerificationData() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.RUNNING, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse);

    validateStateNotification(dataAnalysisResponse);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNotifyStateHandleAsyncV2NoVerificationData() {
    Map<String, DelegateResponseData> dataAnalysisResponse = createDataAnalysisResponse(ExecutionStatus.RUNNING, null);
    createMetaDataExecutionData(ExecutionStatus.RUNNING);
    saveAnalysisContext(dataAnalysisResponse);

    validateStateNotification(dataAnalysisResponse);
  }

  private void validateExecutionResponse(Map<String, DelegateResponseData> dataAnalysisResponse,
      ExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
            .filter(ContinuousVerificationExecutionMetaDataKeys.stateExecutionId, stateExecutionId)
            .get();
    assertThat(continuousVerificationExecutionMetaData.getExecutionStatus()).isEqualTo(executionStatus);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(executionStatus);
    assertThat(executionResponse.getStateExecutionData())
        .isEqualTo(((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
                       .getStateExecutionData());
    continuousVerificationExecutionMetaData =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
            .filter(ContinuousVerificationExecutionMetaDataKeys.stateExecutionId, stateExecutionId)
            .get();
    assertThat(continuousVerificationExecutionMetaData.getExecutionStatus()).isEqualTo(executionStatus);
  }

  private void validateStateNotification(Map<String, DelegateResponseData> dataAnalysisResponse) {
    final String correlationId = ((VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next())
                                     .getStateExecutionData()
                                     .getCorrelationId();

    ExecutionStatus executionStatus = random.nextBoolean() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    continuousVerificationService.notifyWorkflowVerificationState(appId, stateExecutionId, executionStatus);
    final NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, correlationId);
    assertThat(notifyResponse).isNotNull();
    VerificationDataAnalysisResponse verificationDataAnalysisResponse =
        (VerificationDataAnalysisResponse) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
    assertThat(verificationDataAnalysisResponse.getExecutionStatus()).isEqualTo(executionStatus);
    assertThat(verificationDataAnalysisResponse.getStateExecutionData().getStatus()).isEqualTo(executionStatus);

    final ExecutionResponse executionResponse = appDynamicsState.handleAsyncResponse(
        executionContext, Collections.singletonMap(generateUuid(), verificationDataAnalysisResponse));
    assertThat(executionResponse.getStateExecutionData())
        .isEqualTo(verificationDataAnalysisResponse.getStateExecutionData());
  }

  private ContinuousVerificationExecutionMetaData createMetaDataExecutionData(ExecutionStatus executionStatus) {
    String uuId = wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                                            .stateExecutionId(stateExecutionId)
                                            .executionStatus(executionStatus)
                                            .build());
    return wingsPersistence.get(ContinuousVerificationExecutionMetaData.class, uuId);
  }

  private Map<String, DelegateResponseData> createDataAnalysisResponse(
      ExecutionStatus executionStatus, String message) {
    final VerificationStateAnalysisExecutionData stateAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .correlationId(generateUuid())
            .stateExecutionInstanceId(stateExecutionId)
            .baselineExecutionId(generateUuid())
            .serverConfigId(generateUuid())
            .canaryNewHostNames(Sets.newHashSet(generateUuid(), generateUuid()))
            .lastExecutionNodes(Sets.newHashSet(generateUuid(), generateUuid(), generateUuid()))
            .analysisMinute(5)
            .query(generateUuid())
            .comparisonStrategy(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS)
            .build();

    stateAnalysisExecutionData.setErrorMsg(message);
    stateAnalysisExecutionData.setStateType(StateType.APP_DYNAMICS.name());
    stateAnalysisExecutionData.setStatus(executionStatus);
    return Collections.singletonMap(generateUuid(),
        VerificationDataAnalysisResponse.builder()
            .executionStatus(executionStatus)
            .stateExecutionData(stateAnalysisExecutionData)
            .build());
  }

  private void saveAnalysisContext(Map<String, DelegateResponseData> dataAnalysisResponse, int... optionalTolerance) {
    int tolerence = optionalTolerance.length > 0 ? optionalTolerance[0] : 3;
    VerificationDataAnalysisResponse verificationDataAnalysisResponse =
        (VerificationDataAnalysisResponse) dataAnalysisResponse.values().iterator().next();
    final AnalysisContext analysisContext =
        AnalysisContext.builder()
            .correlationId(verificationDataAnalysisResponse.getStateExecutionData().getCorrelationId())
            .stateExecutionId(stateExecutionId)
            .prevWorkflowExecutionId(verificationDataAnalysisResponse.getStateExecutionData().getBaselineExecutionId())
            .analysisServerConfigId(verificationDataAnalysisResponse.getStateExecutionData().getServerConfigId())
            .query(verificationDataAnalysisResponse.getStateExecutionData().getQuery())
            .comparisonStrategy(verificationDataAnalysisResponse.getStateExecutionData().getComparisonStrategy())
            .stateType(StateType.valueOf(verificationDataAnalysisResponse.getStateExecutionData().getStateType()))
            .appId(appId)
            .tolerance(tolerence)
            .timeDuration(10)
            .build();

    Map<String, String> controlNodes = new HashMap<>();
    verificationDataAnalysisResponse.getStateExecutionData().getLastExecutionNodes().forEach(
        node -> controlNodes.put(node, DEFAULT_GROUP_NAME));
    Map<String, String> testNodes = new HashMap<>();
    verificationDataAnalysisResponse.getStateExecutionData().getCanaryNewHostNames().forEach(
        node -> testNodes.put(node, DEFAULT_GROUP_NAME));
    analysisContext.setControlNodes(controlNodes);
    analysisContext.setTestNodes(testNodes);
    wingsPersistence.save(analysisContext);
  }

  private void saveMetricAnalysisRecord(RiskLevel... riskLevel) {
    List<NewRelicMetricAnalysis> analyses = new ArrayList<>();
    Arrays.asList(riskLevel).forEach(
        risk -> { analyses.add(NewRelicMetricAnalysis.builder().metricName(generateUuid()).riskLevel(risk).build()); });
    wingsPersistence.save(NewRelicMetricAnalysisRecord.builder()
                              .analysisMinute(5)
                              .appId(appId)
                              .stateExecutionId(stateExecutionId)
                              .metricAnalyses(analyses)
                              .build());
  }
}
