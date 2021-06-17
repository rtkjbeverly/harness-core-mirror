package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.BASE_COMMIT_SHA;
import static io.harness.ngtriggers.Constants.BRANCH;
import static io.harness.ngtriggers.Constants.COMMIT_SHA;
import static io.harness.ngtriggers.Constants.CUSTOM_TYPE;
import static io.harness.ngtriggers.Constants.EVENT;
import static io.harness.ngtriggers.Constants.GIT_USER;
import static io.harness.ngtriggers.Constants.HEADER;
import static io.harness.ngtriggers.Constants.PR;
import static io.harness.ngtriggers.Constants.PR_NUMBER;
import static io.harness.ngtriggers.Constants.PR_TITLE;
import static io.harness.ngtriggers.Constants.PUSH;
import static io.harness.ngtriggers.Constants.REPO_URL;
import static io.harness.ngtriggers.Constants.SCHEDULED_TYPE;
import static io.harness.ngtriggers.Constants.SOURCE_BRANCH;
import static io.harness.ngtriggers.Constants.SOURCE_TYPE;
import static io.harness.ngtriggers.Constants.TARGET_BRANCH;
import static io.harness.ngtriggers.Constants.TYPE;
import static io.harness.ngtriggers.Constants.WEBHOOK_TYPE;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.AWS_CODECOMMIT;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.BITBUCKET;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.CUSTOM;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITHUB;
import static io.harness.ngtriggers.beans.source.WebhookTriggerType.GITLAB;
import static io.harness.pms.contracts.triggers.SourceType.AWS_CODECOMMIT_REPO;
import static io.harness.pms.contracts.triggers.SourceType.BITBUCKET_REPO;
import static io.harness.pms.contracts.triggers.SourceType.CUSTOM_REPO;
import static io.harness.pms.contracts.triggers.SourceType.GITHUB_REPO;
import static io.harness.pms.contracts.triggers.SourceType.GITLAB_REPO;
import static io.harness.pms.contracts.triggers.Type.SCHEDULED;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.SourceType;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.User;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class TriggerHelper {
  public Map<String, Object> buildJsonObjectFromAmbiance(TriggerPayload triggerPayload) {
    Map<String, Object> jsonObject = new HashMap<>();
    ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
    // branchesxv
    switch (parsedPayload.getPayloadCase()) {
      case PR:
        PullRequest pullRequest = parsedPayload.getPr().getPr();
        jsonObject.put(BRANCH, pullRequest.getTarget());
        jsonObject.put(TARGET_BRANCH, pullRequest.getTarget());
        jsonObject.put(SOURCE_BRANCH, pullRequest.getSource());
        jsonObject.put(EVENT, PR);
        jsonObject.put(PR_NUMBER, Long.toString(pullRequest.getNumber()));
        jsonObject.put(PR_TITLE, pullRequest.getTitle());
        jsonObject.put(COMMIT_SHA, pullRequest.getSha());
        if (pullRequest.getBase() != null) {
          jsonObject.put(BASE_COMMIT_SHA, pullRequest.getBase().getSha());
        }
        jsonObject.put(TYPE, WEBHOOK_TYPE);
        jsonObject.put(REPO_URL, parsedPayload.getPr().getRepo().getLink());
        jsonObject.put(GIT_USER, getGitUser(parsedPayload));
        break;
      case PUSH:
        jsonObject.put(BRANCH, parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put(TARGET_BRANCH, parsedPayload.getPush().getRepo().getBranch());
        jsonObject.put(COMMIT_SHA, parsedPayload.getPush().getAfter());
        jsonObject.put(EVENT, PUSH);
        jsonObject.put(TYPE, WEBHOOK_TYPE);
        jsonObject.put(REPO_URL, parsedPayload.getPush().getRepo().getLink());
        jsonObject.put(GIT_USER, parsedPayload.getPush().getSender().getLogin());
        break;
      default:
        if (SCHEDULED == triggerPayload.getType()) {
          jsonObject.put(TYPE, SCHEDULED_TYPE);
        } else {
          jsonObject.put(TYPE, CUSTOM_TYPE);
        }
        break;
    }

    setSourceType(jsonObject, triggerPayload);
    jsonObject.put(HEADER, triggerPayload.getHeadersMap());
    return jsonObject;
  }

  private String getGitUser(ParsedPayload parsedPayload) {
    User sender = parsedPayload.getPr().getSender();
    String gitUser = sender.getLogin();

    if (isBlank(gitUser)) {
      gitUser = sender.getName();
    }

    return gitUser;
  }

  private void setSourceType(Map<String, Object> jsonObject, TriggerPayload triggerPayload) {
    SourceType sourceRepo = triggerPayload.getSourceType();
    String sourceTypeVal = null;
    if (sourceRepo == GITHUB_REPO) {
      sourceTypeVal = GITHUB.getValue();
    } else if (sourceRepo == GITLAB_REPO) {
      sourceTypeVal = GITLAB.getValue();
    } else if (sourceRepo == BITBUCKET_REPO) {
      sourceTypeVal = BITBUCKET.getValue();
    } else if (sourceRepo == CUSTOM_REPO) {
      sourceTypeVal = CUSTOM.getValue();
    } else if (sourceRepo == AWS_CODECOMMIT_REPO) {
      sourceTypeVal = AWS_CODECOMMIT.getValue();
    }

    if (isNotBlank(sourceTypeVal)) {
      jsonObject.put(SOURCE_TYPE, sourceTypeVal);
    }
  }
}