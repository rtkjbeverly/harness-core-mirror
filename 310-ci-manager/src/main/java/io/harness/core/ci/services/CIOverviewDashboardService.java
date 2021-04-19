package io.harness.core.ci.services;

import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildCount;
import io.harness.app.beans.entities.BuildFailureInfo;
import io.harness.app.beans.entities.BuildHealth;
import io.harness.app.beans.entities.DashboardBuildExecutionInfo;
import io.harness.app.beans.entities.DashboardBuildsHealthInfo;

import java.util.List;

public interface CIOverviewDashboardService {
  long totalBuilds(String accountId, String orgId, String projectId, String startInterval, String endInterval);

  long successfulBuilds(String accountId, String orgId, String projectId, String startInterval, String endInterval);

  long failedBuilds(String accountId, String orgId, String projectId, String startInterval, String endInterval);

  BuildHealth getCountAndRate(long currentCount, long previousCount);

  BuildCount getBuildCountFromDate(String accountId, String orgId, String projectId, String date);

  DashboardBuildsHealthInfo getDashBoardBuildHealthInfoWithRate(String accountId, String orgId, String projectId,
      String startInterval, String endInterval, String previousStartInterval);

  DashboardBuildExecutionInfo getBuildExecutionBetweenIntervals(
      String accountId, String orgId, String projectId, String startInterval, String endInterval);

  List<BuildFailureInfo> getDashboardBuildFailureInfo(String accountId, String orgId, String projectId, long days);

  List<BuildActiveInfo> getDashboardBuildActiveInfo(String accountId, String orgId, String projectId, long days);
}