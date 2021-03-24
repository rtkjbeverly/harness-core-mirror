package io.harness.failureStrategy;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.yaml.core.failurestrategy.NGFailureType.ANY_OTHER_ERRORS;
import static io.harness.yaml.core.failurestrategy.NGFailureType.AUTHORIZATION_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.CONNECTIVITY_ERROR;
import static io.harness.yaml.core.failurestrategy.NGFailureType.TIMEOUT_ERROR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.FailureStrategiesUtils;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.abort.AbortFailureActionConfig;
import io.harness.yaml.core.failurestrategy.ignore.IgnoreFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureSpecConfig;
import io.harness.yaml.core.timeout.Timeout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class StageFailureStrategyTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAtleastAnyOtherFailureStrategyExists() {
    List<FailureStrategyConfig> stageFailureStrategies1, stageFailureStrategies2, stageFailureStrategies3;
    // Not containing error type as ANY_OTHER_ERRORS
    stageFailureStrategies1 = Collections.singletonList(
        FailureStrategyConfig.builder()
            .onFailure(OnFailureConfig.builder()
                           .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                           .action(RetryFailureActionConfig.builder()
                                       .specConfig(RetryFailureSpecConfig.builder()
                                                       .retryCount(ParameterField.createValueField(4))
                                                       .retryIntervals(ParameterField.createValueField(
                                                           Collections.singletonList(Timeout.fromString("2s"))))
                                                       .build())
                                       .build())
                           .build())
            .build());

    boolean ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies1);
    assertThat(ans).isEqualTo(false);

    // Containing error type as ANY_OTHER_ERRORS only
    stageFailureStrategies2 =
        Collections.singletonList(FailureStrategyConfig.builder()
                                      .onFailure(OnFailureConfig.builder()
                                                     .errors(Collections.singletonList(ANY_OTHER_ERRORS))
                                                     .action(AbortFailureActionConfig.builder().build())
                                                     .build())
                                      .build());

    ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies2);
    assertThat(ans).isEqualTo(true);

    // Containing other error along with ANY_OTHER_ERRORS
    List<NGFailureType> test = new ArrayList<>();
    test.add(AUTHORIZATION_ERROR);
    test.add(ANY_OTHER_ERRORS);
    stageFailureStrategies3 = Collections.singletonList(
        FailureStrategyConfig.builder()
            .onFailure(
                OnFailureConfig.builder().errors(test).action(AbortFailureActionConfig.builder().build()).build())
            .build());
    ans = new GenericStepPMSPlanCreator() {
      @Override
      public Set<String> getSupportedStepTypes() {
        return null;
      }
    }.containsOnlyAnyOtherError(stageFailureStrategies3);
    assertThat(ans).isEqualTo(false);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testMultipleActionsSingleError() {
    List<FailureStrategyConfig> stageFailureStrategies = new ArrayList<>();
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                                                  .action(AbortFailureActionConfig.builder().build())
                                                  .build())
                                   .build());
    stageFailureStrategies.add(FailureStrategyConfig.builder()
                                   .onFailure(OnFailureConfig.builder()
                                                  .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                                                  .action(IgnoreFailureActionConfig.builder().build())
                                                  .build())
                                   .build());

    assertThatThrownBy(() -> FailureStrategiesUtils.priorityMergeFailureStrategies(null, null, stageFailureStrategies))
        .isInstanceOf(InvalidRequestException.class);

    List<NGFailureType> test = new ArrayList<>();
    test.add(AUTHORIZATION_ERROR);
    test.add(ANY_OTHER_ERRORS);
    List<FailureStrategyConfig> stageFailureStrategies2 = new ArrayList<>();
    stageFailureStrategies2.add(
        FailureStrategyConfig.builder()
            .onFailure(
                OnFailureConfig.builder().errors(test).action(AbortFailureActionConfig.builder().build()).build())
            .build());
    stageFailureStrategies2.add(FailureStrategyConfig.builder()
                                    .onFailure(OnFailureConfig.builder()
                                                   .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                                                   .action(IgnoreFailureActionConfig.builder().build())
                                                   .build())
                                    .build());

    assertThatThrownBy(() -> FailureStrategiesUtils.priorityMergeFailureStrategies(null, null, stageFailureStrategies2))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testMultipleErrorsSingleAction() {
    List<FailureStrategyConfig> stageFailureStrategies1 = new ArrayList<>();
    stageFailureStrategies1.add(FailureStrategyConfig.builder()
                                    .onFailure(OnFailureConfig.builder()
                                                   .errors(Collections.singletonList(AUTHORIZATION_ERROR))
                                                   .action(IgnoreFailureActionConfig.builder().build())
                                                   .build())
                                    .build());
    stageFailureStrategies1.add(FailureStrategyConfig.builder()
                                    .onFailure(OnFailureConfig.builder()
                                                   .errors(Collections.singletonList(CONNECTIVITY_ERROR))
                                                   .action(IgnoreFailureActionConfig.builder().build())
                                                   .build())
                                    .build());
    stageFailureStrategies1.add(FailureStrategyConfig.builder()
                                    .onFailure(OnFailureConfig.builder()
                                                   .errors(Collections.singletonList(TIMEOUT_ERROR))
                                                   .action(IgnoreFailureActionConfig.builder().build())
                                                   .build())
                                    .build());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMapExpected =
        FailureStrategiesUtils.priorityMergeFailureStrategies(null, null, stageFailureStrategies1);

    List<NGFailureType> test = new ArrayList<>();
    test.add(AUTHORIZATION_ERROR);
    test.add(CONNECTIVITY_ERROR);
    test.add(TIMEOUT_ERROR);
    FailureStrategyActionConfig testAction = IgnoreFailureActionConfig.builder().build();
    List<FailureStrategyConfig> stageFailureStrategies2 = new ArrayList<>();
    stageFailureStrategies2.add(FailureStrategyConfig.builder()
                                    .onFailure(OnFailureConfig.builder().errors(test).action(testAction).build())
                                    .build());

    Map<FailureStrategyActionConfig, Collection<FailureType>> actionMapResult =
        FailureStrategiesUtils.priorityMergeFailureStrategies(null, null, stageFailureStrategies2);

    assertThat(actionMapExpected.keySet().equals(actionMapResult.keySet())).isEqualTo(true);
    assertThat(actionMapExpected.get(testAction).equals(actionMapResult.get(testAction))).isEqualTo(true);
  }
}