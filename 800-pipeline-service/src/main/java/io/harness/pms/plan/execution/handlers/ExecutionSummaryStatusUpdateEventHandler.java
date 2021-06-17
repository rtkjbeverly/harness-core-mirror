package io.harness.pms.plan.execution.handlers;

import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.PIPELINE;
import static io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup.STAGE;

import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.event.GraphNodeUpdateInfo;
import io.harness.event.GraphNodeUpdateObserver;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.steps.StepSpecTypeConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Singleton
public class ExecutionSummaryStatusUpdateEventHandler
    implements NodeStatusUpdateObserver, GraphNodeUpdateObserver, AsyncInformObserver {
  @Inject @Named("PipelineExecutorService") private ExecutorService executorService;
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    updatePipelineLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
    updateStageLevelInfo(nodeUpdateInfo.getPlanExecutionId(), nodeUpdateInfo.getNodeExecution());
  }

  @Override
  public void update(GraphNodeUpdateInfo graphNodeUpdateInfo) {
    String planExecutionId = graphNodeUpdateInfo.getPlanExecutionId();
    Update update = new Update();
    for (NodeExecution nodeExecution : graphNodeUpdateInfo.getNodeExecutions()) {
      if (Objects.equals(nodeExecution.getNode().getGroup(), STAGE.name())
          || Objects.equals(nodeExecution.getNode().getStepType().getType(), StepSpecTypeConstants.BARRIER)) {
        ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
      }
    }
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  public void updatePipelineLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (Objects.equals(nodeExecution.getNode().getGroup(), PIPELINE.name())) {
      Update update = new Update();
      ExecutionSummaryUpdateUtils.addPipelineUpdateCriteria(update, planExecutionId, nodeExecution);
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  public void updateStageLevelInfo(String planExecutionId, NodeExecution nodeExecution) {
    if (Objects.equals(nodeExecution.getNode().getGroup(), STAGE.name())
        || Objects.equals(nodeExecution.getNode().getStepType().getType(), StepSpecTypeConstants.BARRIER)) {
      Update update = new Update();
      ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
      Criteria criteria =
          Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}