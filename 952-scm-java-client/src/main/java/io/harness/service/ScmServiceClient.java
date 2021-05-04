package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import java.util.List;

@OwnedBy(DX)
public interface ScmServiceClient {
  CreateFileResponse createFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  UpdateFileResponse updateFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  DeleteFileResponse deleteFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent getFileContent(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent getLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  IsLatestFileResponse isLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails,
      FileContent fileContent, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent pushFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInBranchResponse findFilesInBranch(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  GetLatestCommitResponse getLatestCommit(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListBranchesResponse listBranches(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListCommitsResponse listCommits(ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileBatchContentResponse listFiles(
      ScmConnector connector, List<String> filePaths, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  void createNewBranch(
      ScmConnector scmConnector, String branch, String defaultBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub);
}