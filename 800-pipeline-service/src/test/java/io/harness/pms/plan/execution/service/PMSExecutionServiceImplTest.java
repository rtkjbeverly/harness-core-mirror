package io.harness.pms.plan.execution.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.mongodb.client.result.UpdateResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public class PMSExecutionServiceImplTest extends PipelineServiceTestBase {
  @Mock private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Mock private UpdateResult updateResult;
  @InjectMocks private PMSExecutionServiceImpl pmsExecutionService;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "basichttpFail";
  private final String PLAN_EXECUTION_ID = "planId";
  private final String INVALID_PLAN_EXECUTION_ID = "InvalidPlanId";
  private final Boolean PIPELINE_DELETED = Boolean.FALSE;
  private String inputSetYaml;
  private String pipelineYaml;

  PipelineExecutionSummaryEntity executionSummaryEntity;
  PipelineEntity pipelineEntity;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    String inputSetFilename = "inputSet1.yml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFilename)), StandardCharsets.UTF_8);

    executionSummaryEntity = PipelineExecutionSummaryEntity.builder()
                                 .accountId(ACCOUNT_ID)
                                 .orgIdentifier(ORG_IDENTIFIER)
                                 .projectIdentifier(PROJ_IDENTIFIER)
                                 .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                 .planExecutionId(PLAN_EXECUTION_ID)
                                 .name(PLAN_EXECUTION_ID)
                                 .runSequence(0)
                                 .inputSetYaml(inputSetYaml)
                                 .build();

    String pipelineYamlFileName = "failure-strategy.yaml";
    pipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(pipelineYamlFileName)), StandardCharsets.UTF_8);

    pipelineEntity = PipelineEntity.builder()
                         .accountId(ACCOUNT_ID)
                         .orgIdentifier(ORG_IDENTIFIER)
                         .projectIdentifier(PROJ_IDENTIFIER)
                         .identifier(PIPELINE_IDENTIFIER)
                         .name(PIPELINE_IDENTIFIER)
                         .yaml(pipelineYaml)
                         .build();
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testFormCriteria() {
    Criteria form = pmsExecutionService.formCriteria(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER,
        null, null, null, null, null, false, !PIPELINE_DELETED);

    assertThat(form.getCriteriaObject().get("accountId").toString().contentEquals(ACCOUNT_ID)).isEqualTo(true);
    assertThat(form.getCriteriaObject().get("orgIdentifier").toString().contentEquals(ORG_IDENTIFIER)).isEqualTo(true);
    assertThat(form.getCriteriaObject().get("projectIdentifier").toString().contentEquals(PROJ_IDENTIFIER))
        .isEqualTo(true);
    assertThat(form.getCriteriaObject().get("pipelineIdentifier").toString().contentEquals(PIPELINE_IDENTIFIER))
        .isEqualTo(true);
    assertThat(form.getCriteriaObject().containsKey("status")).isEqualTo(false);
    assertThat(form.getCriteriaObject().get("pipelineDeleted")).isNotEqualTo(true);
    assertThat(form.getCriteriaObject().containsKey("executionTriggerInfo")).isEqualTo(false);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetInputSetYaml() {
    doReturn(Optional.of(executionSummaryEntity))
        .when(pmsExecutionSummaryRepository)
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PLAN_EXECUTION_ID, !PIPELINE_DELETED);

    String inputSet = pmsExecutionService.getInputSetYaml(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PLAN_EXECUTION_ID, PIPELINE_DELETED);

    assertThat(inputSet).isEqualTo(inputSetYaml);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetInputSetYamlWithInvalidExecutionId() {
    doReturn(Optional.empty())
        .when(pmsExecutionSummaryRepository)
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, INVALID_PLAN_EXECUTION_ID, !PIPELINE_DELETED);

    assertThatThrownBy(()
                           -> pmsExecutionService.getInputSetYaml(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER,
                               INVALID_PLAN_EXECUTION_ID, PIPELINE_DELETED))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid request : Input Set did not exist or pipeline execution has been deleted");
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetPipelineExecutionSummaryEntity() {
    doReturn(Optional.of(executionSummaryEntity))
        .when(pmsExecutionSummaryRepository)
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PLAN_EXECUTION_ID, !PIPELINE_DELETED);

    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        pmsExecutionService.getPipelineExecutionSummaryEntity(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PLAN_EXECUTION_ID, PIPELINE_DELETED);

    assertThat(pipelineExecutionSummaryEntity).isEqualTo(executionSummaryEntity);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetPipelineExecutionSummaryEntityWithInvalidExecutionId() {
    doReturn(Optional.empty())
        .when(pmsExecutionSummaryRepository)
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionIdAndPipelineDeletedNot(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, INVALID_PLAN_EXECUTION_ID, !PIPELINE_DELETED);

    assertThatThrownBy(()
                           -> pmsExecutionService.getPipelineExecutionSummaryEntity(ACCOUNT_ID, ORG_IDENTIFIER,
                               PROJ_IDENTIFIER, INVALID_PLAN_EXECUTION_ID, PIPELINE_DELETED))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Plan Execution Summary does not exist or has been deleted for given planExecutionId");
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testDeleteExecutionsOnPipelineDeletion() {
    Criteria criteria = new Criteria();
    criteria.and("accountId")
        .is(ACCOUNT_ID)
        .and("orgIdentifier")
        .is(ORG_IDENTIFIER)
        .and("projectIdentifier")
        .is(PROJ_IDENTIFIER)
        .and("pipelineIdentifier")
        .is(PIPELINE_IDENTIFIER);
    Query query = new Query(criteria);

    Update update = new Update();
    update.set("pipelineDeleted", Boolean.TRUE);

    doReturn(true).when(updateResult).wasAcknowledged();
    doReturn(updateResult).when(pmsExecutionSummaryRepository).deleteAllExecutionsWhenPipelineDeleted(query, update);

    pmsExecutionService.deleteExecutionsOnPipelineDeletion(pipelineEntity);

    verify(pmsExecutionSummaryRepository, times(1)).deleteAllExecutionsWhenPipelineDeleted(query, update);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testDeleteExecutionsOnPipelineDeletionWhenDeleteFailed() {
    Criteria criteria = new Criteria();
    criteria.and("accountId")
        .is(ACCOUNT_ID)
        .and("orgIdentifier")
        .is(ORG_IDENTIFIER)
        .and("projectIdentifier")
        .is(PROJ_IDENTIFIER)
        .and("pipelineIdentifier")
        .is(PIPELINE_IDENTIFIER);
    Query query = new Query(criteria);

    Update update = new Update();
    update.set("pipelineDeleted", Boolean.TRUE);

    doReturn(false).when(updateResult).wasAcknowledged();
    doReturn(updateResult).when(pmsExecutionSummaryRepository).deleteAllExecutionsWhenPipelineDeleted(query, update);

    assertThatThrownBy(() -> pmsExecutionService.deleteExecutionsOnPipelineDeletion(pipelineEntity))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            String.format("Executions for Pipeline [%s] under Project[%s], Organization [%s] couldn't be deleted.",
                PIPELINE_IDENTIFIER, PROJ_IDENTIFIER, ORG_IDENTIFIER));
  }
}