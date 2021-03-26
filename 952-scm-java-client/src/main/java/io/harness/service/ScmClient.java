package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.ContentResponse;

@OwnedBy(DX)
public interface ScmClient {
  ContentResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);
}