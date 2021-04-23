package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardBuildRepositoryInfo {
  private List<RepositoryInfo> repositoryInfo;
}