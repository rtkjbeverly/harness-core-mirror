package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFileDetails.GitFileDetailsBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.helpers.ScmUserHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.scm.beans.ScmCreateFileResponse;
import io.harness.gitsync.scm.beans.ScmDeleteFileResponse;
import io.harness.gitsync.scm.beans.ScmUpdateFileResponse;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class ScmGitUtils {
  public GitFileDetailsBuilder getGitFileDetails(GitEntityInfo gitEntityInfo, String yaml) {
    final EmbeddedUser currentUser = ScmUserHelper.getCurrentUser();
    String filePath = createFilePath(gitEntityInfo.getFolderPath(), gitEntityInfo.getFilePath());
    return GitFileDetails.builder()
        .branch(gitEntityInfo.getBranch())
        .commitMessage(
            isEmpty(gitEntityInfo.getCommitMsg()) ? GitSyncConstants.COMMIT_MSG : gitEntityInfo.getCommitMsg())
        .fileContent(yaml)
        .filePath(filePath)
        .userEmail(currentUser.getEmail())
        .userName(currentUser.getName());
  }

  public String createFilePath(String folderPath, String filePath) {
    if (isEmpty(folderPath)) {
      throw new InvalidRequestException("Folder path cannot be empty");
    }
    if (isEmpty(filePath)) {
      throw new InvalidRequestException("File path cannot be empty");
    }
    String updatedFolderPath = folderPath.endsWith("/") ? folderPath : folderPath.concat("/");
    String folderPathWithoutStartingSlash =
        updatedFolderPath.charAt(0) != '/' ? updatedFolderPath : updatedFolderPath.substring(1);
    String updatedFilePath = filePath.charAt(0) != '/' ? filePath : filePath.substring(1);
    return folderPathWithoutStartingSlash + updatedFilePath;
  }

  public ScmCreateFileResponse createScmCreateFileResponse(String yaml, InfoForGitPush infoForPush) {
    return ScmCreateFileResponse.builder()
        .folderPath(infoForPush.getFolderPath())
        .filePath(infoForPush.getFilePath())
        .pushToDefaultBranch(infoForPush.isDefault())
        .yamlGitConfigId(infoForPush.getYamlGitConfigId())
        .accountIdentifier(infoForPush.getAccountId())
        .orgIdentifier(infoForPush.getOrgIdentifier())
        .projectIdentifier(infoForPush.getProjectIdentifier())
        .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
        .branch(infoForPush.getBranch())
        .build();
  }

  public ScmUpdateFileResponse createScmUpdateFileResponse(String yaml, InfoForGitPush infoForPush) {
    return ScmUpdateFileResponse.builder()
        .folderPath(infoForPush.getFolderPath())
        .filePath(infoForPush.getFilePath())
        .pushToDefaultBranch(infoForPush.isDefault())
        .yamlGitConfigId(infoForPush.getYamlGitConfigId())
        .accountIdentifier(infoForPush.getAccountId())
        .orgIdentifier(infoForPush.getOrgIdentifier())
        .projectIdentifier(infoForPush.getProjectIdentifier())
        .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
        .branch(infoForPush.getBranch())
        .build();
  }

  public ScmDeleteFileResponse createScmDeleteFileResponse(String yaml, InfoForGitPush infoForPush) {
    return ScmDeleteFileResponse.builder()
        .folderPath(infoForPush.getFolderPath())
        .filePath(infoForPush.getFilePath())
        .pushToDefaultBranch(infoForPush.isDefault())
        .yamlGitConfigId(infoForPush.getYamlGitConfigId())
        .accountIdentifier(infoForPush.getAccountId())
        .orgIdentifier(infoForPush.getOrgIdentifier())
        .projectIdentifier(infoForPush.getProjectIdentifier())
        .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
        .branch(infoForPush.getBranch())
        .build();
  }
}