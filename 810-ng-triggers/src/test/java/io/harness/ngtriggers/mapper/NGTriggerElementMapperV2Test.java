package io.harness.ngtriggers.mapper;

import static io.harness.ngtriggers.beans.source.NGTriggerType.SCHEDULED;
import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.CONTAINS;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.ENDS_WITH;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.EQUALS;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.IN;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.NOT_EQUALS;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.NOT_IN;
import static io.harness.ngtriggers.conditionchecker.ConditionOperator.STARTS_WITH;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.MATT;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.USE_NATIVE_TYPE_ID;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.NGTriggerDetailsResponseDTO;
import io.harness.ngtriggers.beans.dto.NGTriggerResponseDTO;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerEventHistory;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.custom.CustomTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.GitlabSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;
import io.harness.repositories.spring.TriggerEventHistoryRepository;
import io.harness.rule.Owner;
import io.harness.webhook.WebhookConfigProvider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGTriggerElementMapperV2Test extends CategoryTest {
  private String ngTriggerYaml_github_pr;
  private String ngTriggerYaml_github_push;
  private String ngTriggerYaml_github_issue_comment;

  private String ngTriggerYaml_gitlab_pr;
  private String ngTriggerYaml_gitlab_push;

  private String ngTriggerYaml_bitbucket_pr;
  private String ngTriggerYaml_bitbucket_push;

  private String ngTriggerYaml_awscodecommit_push;
  private String ngTriggerYaml_custom;
  private String ngTriggerYaml_cron;

  private List<TriggerEventDataCondition> payloadConditions;
  private List<TriggerEventDataCondition> headerConditions;
  private static final String inputYaml = "pipeline:\n"
      + "  identifier: secrethttp1\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: qaStage\n"
      + "        spec:\n"
      + "          infrastructure:\n"
      + "            infrastructureDefinition:\n"
      + "              spec:\n"
      + "                releaseName: releaseName1";
  private static final String JEXL = "true";
  private static final String REPO = "myrepo";
  private static final String CONN = "conn";
  @Mock private TriggerEventHistoryRepository triggerEventHistoryRepository;
  @Mock private WebhookConfigProvider webhookConfigProvider;
  @InjectMocks @Inject private NGTriggerElementMapper ngTriggerElementMapper;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    ngTriggerYaml_github_push = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-github-push-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_github_pr = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-github-pr-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_github_issue_comment =
        Resources.toString(Objects.requireNonNull(classLoader.getResource("ng-trigger-github-issue-comment-v2.yaml")),
            StandardCharsets.UTF_8);
    ngTriggerYaml_gitlab_pr = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-gitlab-pr-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_gitlab_push = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-gitlab-push-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_bitbucket_pr = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-bitbucket-pr-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_bitbucket_push = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-bitbucket-push-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_awscodecommit_push =
        Resources.toString(Objects.requireNonNull(classLoader.getResource("ng-trigger-awscodecommit-push-v2.yaml")),
            StandardCharsets.UTF_8);
    ngTriggerYaml_custom = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-custom-v2.yaml")), StandardCharsets.UTF_8);
    ngTriggerYaml_cron = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("ng-trigger-cron-v2.yaml")), StandardCharsets.UTF_8);

    payloadConditions = asList(TriggerEventDataCondition.builder().key("k1").operator(EQUALS).value("v1").build(),
        TriggerEventDataCondition.builder().key("k2").operator(NOT_EQUALS).value("v2").build(),
        TriggerEventDataCondition.builder().key("k3").operator(IN).value("v3,c").build(),
        TriggerEventDataCondition.builder().key("k4").operator(NOT_IN).value("v4").build(),
        TriggerEventDataCondition.builder().key("k5").operator(STARTS_WITH).value("v5").build(),
        TriggerEventDataCondition.builder().key("k6").operator(ENDS_WITH).value("v6").build(),
        TriggerEventDataCondition.builder().key("k7").operator(CONTAINS).value("v7").build());
    headerConditions = asList(TriggerEventDataCondition.builder().key("h1").operator(EQUALS).value("v1").build());

    doReturn("https://app.harness.io/pipeline/api")
        .doReturn("https://app.harness.io/pipeline/api/")
        .doReturn("https://app.harness.io/pipeline/api/#")
        .doReturn(null)
        .when(webhookConfigProvider)
        .getPmsApiBaseUrl();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGitubPR() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_github_pr);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.PULL_REQUEST);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.PULL_REQUEST);
    assertThat(githubSpec.fetchGitAware().fetchActions()).containsAll(asList(GithubPRAction.OPEN, GithubPRAction.EDIT));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGitubPush() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_github_push);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.PUSH);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.PUSH);
    assertThat(githubSpec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGitubIssueComment() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_github_issue_comment);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.ISSUE_COMMENT);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.ISSUE_COMMENT);
    assertThat(githubSpec.fetchGitAware().fetchActions())
        .containsAll(asList(GithubIssueCommentAction.CREATE, GithubIssueCommentAction.DELETE));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGilabPR() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_gitlab_pr);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITLAB);
    assertThat(GitlabSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GitlabSpec spec = (GitlabSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.getType()).isEqualTo(GitlabTriggerEvent.MERGE_REQUEST);
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(spec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(spec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(spec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(spec.fetchGitAware().fetchEvent()).isEqualTo(GitlabTriggerEvent.MERGE_REQUEST);
    assertThat(spec.fetchGitAware().fetchActions()).containsAll(asList(GitlabPRAction.OPEN, GitlabPRAction.MERGE));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGilabPush() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_gitlab_push);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITLAB);
    assertThat(GitlabSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GitlabSpec spec = (GitlabSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.getType()).isEqualTo(GitlabTriggerEvent.PUSH);
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(spec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(spec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(spec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(spec.fetchGitAware().fetchEvent()).isEqualTo(GitlabTriggerEvent.PUSH);
    assertThat(spec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testBitbucketPR() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_bitbucket_pr);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.BITBUCKET);
    assertThat(BitbucketSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    BitbucketSpec spec = (BitbucketSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.getType()).isEqualTo(BitbucketTriggerEvent.PULL_REQUEST);
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(spec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(spec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(spec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(spec.fetchGitAware().fetchEvent()).isEqualTo(BitbucketTriggerEvent.PULL_REQUEST);
    assertThat(spec.fetchGitAware().fetchActions())
        .containsAll(asList(BitbucketPRAction.UPDATE, BitbucketPRAction.DECLINE));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testBitbucketPush() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_bitbucket_push);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.BITBUCKET);
    assertThat(BitbucketSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    BitbucketSpec spec = (BitbucketSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.getType()).isEqualTo(BitbucketTriggerEvent.PUSH);
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(spec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(spec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(spec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isTrue();
    assertThat(spec.fetchGitAware().fetchEvent()).isEqualTo(BitbucketTriggerEvent.PUSH);
    assertThat(spec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAwsCodeCommitPush() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_awscodecommit_push);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.AWS_CODECOMMIT);
    assertThat(AwsCodeCommitSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    AwsCodeCommitSpec spec = (AwsCodeCommitSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.getType()).isEqualTo(AwsCodeCommitTriggerEvent.PUSH);
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).isEmpty();
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
    assertThat(spec.fetchGitAware().fetchRepoName()).isEqualTo(REPO);
    assertThat(spec.fetchGitAware().fetchConnectorRef()).isEqualTo(CONN);
    assertThat(spec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(spec.fetchGitAware().fetchEvent()).isEqualTo(AwsCodeCommitTriggerEvent.PUSH);
    assertThat(spec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCustomPush() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_custom);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.CUSTOM);
    assertThat(CustomTriggerSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    CustomTriggerSpec spec = (CustomTriggerSpec) webhookTriggerConfigV2.getSpec();
    assertThat(spec.fetchPayloadAware().fetchPayloadConditions()).containsAll(payloadConditions);
    assertThat(spec.fetchPayloadAware().fetchHeaderConditions()).containsAll(headerConditions);
    assertThat(spec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCron() throws Exception {
    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(ngTriggerYaml_cron);

    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(SCHEDULED);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(ScheduledTriggerConfig.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    ScheduledTriggerConfig scheduledTriggerConfig = (ScheduledTriggerConfig) ngTriggerSpecV2;
    assertThat(scheduledTriggerConfig.getType()).isEqualTo("Cron");
    ScheduledTriggerSpec scheduledTriggerSpec = scheduledTriggerConfig.getSpec();
    assertThat(CronTriggerSpec.class.isAssignableFrom(scheduledTriggerSpec.getClass())).isTrue();
    CronTriggerSpec cronTriggerSpec = (CronTriggerSpec) scheduledTriggerSpec;
    assertThat(cronTriggerSpec.getExpression()).isEqualTo("20 4 * * *");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testYamlConversion() throws Exception {
    String yamlV0 = Resources.toString(
        Objects.requireNonNull(getClass().getClassLoader().getResource("ng-trigger-v0.yaml")), StandardCharsets.UTF_8);

    NGTriggerConfigV2 ngTriggerConfigV2 = ngTriggerElementMapper.toTriggerConfigV2(NGTriggerEntity.builder()
                                                                                       .accountId("acc")
                                                                                       .orgIdentifier("org")
                                                                                       .projectIdentifier("proj")
                                                                                       .identifier("first_trigger")
                                                                                       .ymlVersion(null)
                                                                                       .yaml(yamlV0)
                                                                                       .build());

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()
                                                     .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                                     .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                                     .disable(USE_NATIVE_TYPE_ID));
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    String s = objectMapper.writeValueAsString(ngTriggerConfigV2);

    NGTriggerConfigV2 ngTriggerConfigV3 = ngTriggerElementMapper.toTriggerConfigV2(s);
    int i = 0;
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testTriggerEntityCronHasNextIterations() {
    NGTriggerEntity ngTriggerEntity =
        ngTriggerElementMapper.toTriggerDetails("accId", "orgId", "projId", ngTriggerYaml_cron).getNgTriggerEntity();
    assertThat(ngTriggerEntity.getNextIterations()).isNotEmpty();
    // all elements snap to the nearest minute -- in other words,  now is not an element.
    for (long nextIteration : ngTriggerEntity.getNextIterations()) {
      assertThat(nextIteration % 60000).isEqualTo(0);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTriggerEntityWithWrongIdentifier() {
    assertThatThrownBy(()
                           -> ngTriggerElementMapper.toTriggerEntity(
                               "accId", "orgId", "projId", "not_first_trigger", ngTriggerYaml_gitlab_pr))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPrepareExecutionDataArray() throws Exception {
    String sDate0 = "23-Dec-1998 02:37:50";
    String sDate1 = "24-Dec-1998 02:37:50";
    String sDate2 = "24-Dec-1998 12:37:50";
    String sDate3 = "25-Dec-1998 14:37:50";
    String sDate4 = "25-Dec-1998 15:37:50";
    String sDate5 = "25-Dec-1998 16:37:50";
    String sDate6 = "25-Dec-1998 20:37:50";
    String sDate7 = "26-Dec-1998 01:37:50";
    String sDate8 = "26-Dec-1998 11:12:50";
    String sDate9 = "26-Dec-1998 21:37:50";
    String sDate10 = "26-Dec-1998 22:37:50";
    String sDate11 = "26-Dec-1998 23:37:50";
    String sDate12 = "26-Dec-1998 23:47:50";
    String sDate13 = "27-Dec-1998 02:37:50";
    String sDate14 = "27-Dec-1998 21:37:50";
    String sDate15 = "29-Dec-1998 23:37:50";
    String sDate16 = "29-Dec-1998 13:37:50";
    String sDate17 = "29-Dec-1998 14:37:50";
    String sDate18 = "29-Dec-1998 15:37:50";
    String sDate19 = "29-Dec-1998 16:37:50";
    String sDate20 = "30-Dec-1998 17:37:50";
    String sDate21 = "30-Dec-1998 18:37:50";

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    List<TriggerEventHistory> triggerEventHistories = asList(generateEventHistoryWithTimestamp(formatter, sDate0),
        generateEventHistoryWithTimestamp(formatter, sDate1), generateEventHistoryWithTimestamp(formatter, sDate2),
        generateEventHistoryWithTimestamp(formatter, sDate3), generateEventHistoryWithTimestamp(formatter, sDate4),
        generateEventHistoryWithTimestamp(formatter, sDate5), generateEventHistoryWithTimestamp(formatter, sDate6),
        generateEventHistoryWithTimestamp(formatter, sDate7), generateEventHistoryWithTimestamp(formatter, sDate8),
        generateEventHistoryWithTimestamp(formatter, sDate9), generateEventHistoryWithTimestamp(formatter, sDate10),
        generateEventHistoryWithTimestamp(formatter, sDate11), generateEventHistoryWithTimestamp(formatter, sDate12),
        generateEventHistoryWithTimestamp(formatter, sDate13), generateEventHistoryWithTimestamp(formatter, sDate14),
        generateEventHistoryWithTimestamp(formatter, sDate15), generateEventHistoryWithTimestamp(formatter, sDate16),
        generateEventHistoryWithTimestamp(formatter, sDate17), generateEventHistoryWithTimestamp(formatter, sDate18),
        generateEventHistoryWithTimestamp(formatter, sDate19), generateEventHistoryWithTimestamp(formatter, sDate20),
        generateEventHistoryWithTimestamp(formatter, sDate21));

    Integer[] executionData = ngTriggerElementMapper.prepareExecutionDataArray(
        formatter.parse("30-Dec-1998 21:37:50").getTime(), triggerEventHistories);
    assertThat(executionData).containsExactlyInAnyOrder(2, 5, 0, 2, 6, 4, 2);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testToResponseDTO() {
    NGTriggerEntity ngTriggerEntity =
        ngTriggerElementMapper.toTriggerDetails("accId", "org", "proj", ngTriggerYaml_gitlab_pr).getNgTriggerEntity();
    NGTriggerResponseDTO responseDTO = ngTriggerElementMapper.toResponseDTO(ngTriggerEntity);
    assertThat(responseDTO.getAccountIdentifier()).isEqualTo(ngTriggerEntity.getAccountId());
    assertThat(responseDTO.getTargetIdentifier()).isEqualTo(ngTriggerEntity.getTargetIdentifier());
    assertThat(responseDTO.getOrgIdentifier()).isEqualTo(ngTriggerEntity.getOrgIdentifier());
    assertThat(responseDTO.getProjectIdentifier()).isEqualTo(ngTriggerEntity.getProjectIdentifier());
    assertThat(responseDTO.getYaml()).isEqualTo(ngTriggerEntity.getYaml());
    assertThat(responseDTO.getType()).isEqualTo(ngTriggerEntity.getType());
    assertThat(responseDTO.getName()).isEqualTo(ngTriggerEntity.getName());
    assertThat(responseDTO.getIdentifier()).isEqualTo(ngTriggerEntity.getIdentifier());
    assertThat(responseDTO.isEnabled()).isEqualTo(ngTriggerEntity.getEnabled());
    assertThat(responseDTO.getDescription()).isEqualTo(ngTriggerEntity.getDescription());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testGetWebhookUrl() {
    NGTriggerEntity ngTriggerEntity =
        ngTriggerElementMapper.toTriggerDetails("accId", "org", "proj", ngTriggerYaml_gitlab_pr).getNgTriggerEntity();
    NGTriggerDetailsResponseDTO ngTriggerDetailsResponseDTO =
        ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity, false, true);
    // baseUrl: "https://app.harness.io/pipeline/api"
    assertThat(ngTriggerDetailsResponseDTO.getWebhookUrl())
        .isEqualTo("https://app.harness.io/pipeline/api/webhook/trigger?accountIdentifier=accId");

    // baseUrl: "https://app.harness.io/pipeline/api/"
    ngTriggerEntity = ngTriggerElementMapper.toTriggerDetails("accId", "orgId", "projId", ngTriggerYaml_gitlab_pr)
                          .getNgTriggerEntity();
    ngTriggerDetailsResponseDTO = ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity, false, true);
    assertThat(ngTriggerDetailsResponseDTO.getWebhookUrl())
        .isEqualTo("https://app.harness.io/pipeline/api/webhook/trigger?accountIdentifier=accId");

    // baseUrl: "https://app.harness.io/pipeline/api/#"
    ngTriggerDetailsResponseDTO = ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity, false, true);
    assertThat(ngTriggerDetailsResponseDTO.getWebhookUrl())
        .isEqualTo("https://app.harness.io/pipeline/api/webhook/trigger?accountIdentifier=accId");

    // baseUrl: null
    ngTriggerDetailsResponseDTO = ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity, false, true);
    assertThat(ngTriggerDetailsResponseDTO.getWebhookUrl()).isNull();

    ngTriggerEntity.setType(SCHEDULED);
    ngTriggerDetailsResponseDTO = ngTriggerElementMapper.toNGTriggerDetailsResponseDTO(ngTriggerEntity, false, true);
    assertThat(ngTriggerDetailsResponseDTO.getWebhookUrl()).isNull();
  }

  private TriggerEventHistory generateEventHistoryWithTimestamp(SimpleDateFormat formatter6, String sDate1)
      throws ParseException {
    return TriggerEventHistory.builder().createdAt(formatter6.parse(sDate1).getTime()).build();
  }

  private void assertRootLevelProperties(NGTriggerConfigV2 ngTriggerConfigV2) {
    assertThat(ngTriggerConfigV2).isNotNull();
    assertThat(ngTriggerConfigV2.getIdentifier()).isEqualTo("first_trigger");
    assertThat(ngTriggerConfigV2.getEnabled()).isTrue();
    assertThat(ngTriggerConfigV2.getInputYaml()).isEqualTo(inputYaml);
    assertThat(ngTriggerConfigV2.getPipelineIdentifier()).isEqualTo("pipeline");
    assertThat(ngTriggerConfigV2.getOrgIdentifier()).isEqualTo("org");
    assertThat(ngTriggerConfigV2.getProjectIdentifier()).isEqualTo("proj");
    assertThat(ngTriggerConfigV2.getName()).isEqualTo("first trigger");
  }
}