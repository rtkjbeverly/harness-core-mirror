package io.harness.pms.triggers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.execution.PlanExecution.EXEC_TAG_SET_BY_TRIGGER;
import static io.harness.ngtriggers.Constants.EVENT_CORRELATION_ID;
import static io.harness.ngtriggers.Constants.GIT_USER;
import static io.harness.ngtriggers.Constants.TRIGGER_REF;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.Type;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.product.ci.scm.proto.Repository;
import io.harness.product.ci.scm.proto.User;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class TriggerExecutionHelperTest extends CategoryTest {
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PipelineExecuteHelper pipelineExecuteHelper;
  @Mock PlanExecutionService planExecutionService;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock PMSYamlSchemaService pmsYamlSchemaService;
  @Inject @InjectMocks TriggerExecutionHelper triggerExecutionHelper;

  private NGTriggerEntity ngTriggerEntity;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    ngTriggerEntity = NGTriggerEntity.builder()
                          .accountId("acc")
                          .orgIdentifier("org")
                          .projectIdentifier("proj")
                          .targetIdentifier("target")
                          .identifier("trigger")
                          .build();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateTriggerRef() throws IOException {
    assertThat(triggerExecutionHelper.generateTriggerRef(ngTriggerEntity)).isEqualTo("acc/org/proj/trigger");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateExecutionTagForEvent() throws IOException {
    TriggerDetails triggerDetails = TriggerDetails.builder().ngTriggerEntity(ngTriggerEntity).build();

    TriggerPayload.Builder payloadBuilder = TriggerPayload.newBuilder().setType(Type.GIT).setParsedPayload(
        ParsedPayload.newBuilder()
            .setPr(PullRequestHook.newBuilder()
                       .setPr(PullRequest.newBuilder().setNumber(1).setSource("source").setTarget("target").build())
                       .setRepo(Repository.newBuilder().setLink("https://github.com").build())
                       .build())
            .build());

    String executionTagForEvent =
        triggerExecutionHelper.generateExecutionTagForEvent(triggerDetails, payloadBuilder.build());
    assertThat(executionTagForEvent).isEqualTo("acc:org:proj:target:PR:https://github.com:1:source:target");

    payloadBuilder = TriggerPayload.newBuilder().setType(Type.GIT).setParsedPayload(
        ParsedPayload.newBuilder()
            .setPush(PushHook.newBuilder()
                         .setRepo(Repository.newBuilder().setLink("https://github.com").build())
                         .setRef("ref")
                         .build())
            .build());
    executionTagForEvent = triggerExecutionHelper.generateExecutionTagForEvent(triggerDetails, payloadBuilder.build());
    assertThat(executionTagForEvent).isEqualTo("acc:org:proj:target:PUSH:https://github.com:ref");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateTriggerdBy() throws IOException {
    User user = User.newBuilder().setLogin("login").setEmail("user@email.com").setName("name").build();

    TriggeredBy triggeredBy = triggerExecutionHelper.generateTriggerdBy("tag", ngTriggerEntity,
        TriggerPayload.newBuilder()
            .setParsedPayload(ParsedPayload.newBuilder().setPush(PushHook.newBuilder().setSender(user).build()).build())
            .build(),
        "eventId");

    assertTriggerBy(triggeredBy);

    triggeredBy = triggerExecutionHelper.generateTriggerdBy("tag", ngTriggerEntity,
        TriggerPayload.newBuilder()
            .setParsedPayload(
                ParsedPayload.newBuilder().setPr(PullRequestHook.newBuilder().setSender(user).build()).build())
            .build(),
        "eventId");

    assertTriggerBy(triggeredBy);
  }

  private void assertTriggerBy(TriggeredBy triggeredBy) {
    Map<String, String> extraInfoMap = triggeredBy.getExtraInfoMap();
    assertThat(extraInfoMap.containsKey(EXEC_TAG_SET_BY_TRIGGER)).isTrue();
    assertThat(extraInfoMap.containsKey(TRIGGER_REF)).isTrue();
    assertThat(extraInfoMap.containsKey(EVENT_CORRELATION_ID)).isTrue();

    assertThat(extraInfoMap.get(EXEC_TAG_SET_BY_TRIGGER)).isEqualTo("tag");
    assertThat(extraInfoMap.get(TRIGGER_REF)).isEqualTo("acc/org/proj/trigger");
    assertThat(extraInfoMap.get(GIT_USER)).isEqualTo("login");
    assertThat(extraInfoMap.get(EVENT_CORRELATION_ID)).isEqualTo("eventId");
  }
}