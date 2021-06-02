package io.harness.pms.plan.execution.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.creation.NodeTypeLookupService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.rule.Owner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionSummaryCreateEventHandlerTest extends PipelineServiceTestBase {
  @Mock private PMSPipelineService pmsPipelineService;
  @Mock private PlanExecutionService planExecutionService;
  @Mock private NodeTypeLookupService nodeTypeLookupService;
  @Mock private PmsExecutionSummaryRespository pmsExecutionSummaryRespository;

  private ExecutionSummaryCreateEventHandler executionSummaryCreateEventHandler;

  @Before
  public void setUp() throws Exception {
    executionSummaryCreateEventHandler = new ExecutionSummaryCreateEventHandler(
        pmsPipelineService, planExecutionService, nodeTypeLookupService, pmsExecutionSummaryRespository);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnStart() {
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    PlanExecution planExecution =
        PlanExecution.builder()
            .metadata(ExecutionMetadata.newBuilder()
                          .setRunSequence(1)
                          .setInputSetYaml("some-yaml")
                          .setPipelineIdentifier("pipelineId")
                          .build())
            .plan(Plan.builder()
                      .layoutNodeInfo(GraphLayoutInfo.newBuilder()
                                          .setStartingNodeId("startId")
                                          .putLayoutNodes("startId",
                                              GraphLayoutNode.newBuilder().setNodeGroup("node-group").build())
                                          .build())
                      .build())
            .build();

    PipelineEntity pipelineEntity = PipelineEntity.builder()
                                        .uuid(generateUuid())
                                        .executionSummaryInfo(ExecutionSummaryInfo.builder()
                                                                  .lastExecutionStatus(ExecutionStatus.RUNNING)
                                                                  .numOfErrors(new HashMap<>())
                                                                  .build())
                                        .build();

    ArgumentCaptor<ExecutionSummaryInfo> executionSummaryInfoArgumentCaptor =
        ArgumentCaptor.forClass(ExecutionSummaryInfo.class);

    ArgumentCaptor<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityArgumentCaptor =
        ArgumentCaptor.forClass(PipelineExecutionSummaryEntity.class);

    when(planExecutionService.get(anyString())).thenReturn(planExecution);

    when(pmsPipelineService.get(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
        .thenReturn(Optional.of(pipelineEntity));

    doNothing()
        .when(pmsPipelineService)
        .saveExecutionInfo(
            anyString(), anyString(), anyString(), anyString(), executionSummaryInfoArgumentCaptor.capture());

    when(pmsExecutionSummaryRespository.save(pipelineExecutionSummaryEntityArgumentCaptor.capture())).thenReturn(null);

    when(nodeTypeLookupService.findNodeTypeServiceName(anyString())).thenReturn("pms");

    executionSummaryCreateEventHandler.onStart(ambiance);

    ExecutionSummaryInfo executionSummaryInfo = executionSummaryInfoArgumentCaptor.getValue();
    assertThat(executionSummaryInfo.getLastExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(executionSummaryInfo.getNumOfErrors()).isEmpty();
    assertThat(executionSummaryInfo.getDeployments()).isNotEmpty();
    assertThat(executionSummaryInfo.getDeployments().get(getFormattedDate())).isEqualTo(1);
    assertThat(executionSummaryInfo.getLastExecutionId()).isEqualTo(ambiance.getPlanExecutionId());

    PipelineExecutionSummaryEntity capturedEntity = pipelineExecutionSummaryEntityArgumentCaptor.getValue();
    assertThat(capturedEntity).isNotNull();
    assertThat(capturedEntity.getRunSequence()).isEqualTo(1);
    assertThat(capturedEntity.getPipelineIdentifier()).isEqualTo("pipelineId");
    assertThat(capturedEntity.getPlanExecutionId()).isEqualTo(ambiance.getPlanExecutionId());
    assertThat(capturedEntity.getPipelineDeleted()).isFalse();
    assertThat(capturedEntity.getInternalStatus()).isEqualTo(Status.NO_OP);
    assertThat(capturedEntity.getStatus()).isEqualTo(ExecutionStatus.NOTSTARTED);
    assertThat(capturedEntity.getTags()).isEmpty();
    assertThat(capturedEntity.getStartingNodeId()).isEqualTo("startId");
    assertThat(capturedEntity.getModules()).containsExactly("pms");
    assertThat(capturedEntity.getLayoutNodeMap()).isNotEmpty();
    assertThat(capturedEntity.getLayoutNodeMap()).containsKeys("startId");
  }

  private String getFormattedDate() {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    return formatter.format(date);
  }
}