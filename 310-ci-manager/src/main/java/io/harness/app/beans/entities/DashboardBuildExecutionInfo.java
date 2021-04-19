package io.harness.app.beans.entities;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardBuildExecutionInfo {
  private List<BuildExecutionInfo> buildExecutionInfoList;
}