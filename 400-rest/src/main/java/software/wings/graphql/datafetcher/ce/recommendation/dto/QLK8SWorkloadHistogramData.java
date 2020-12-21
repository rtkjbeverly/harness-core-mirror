package software.wings.graphql.datafetcher.ce.recommendation.dto;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
public class QLK8SWorkloadHistogramData {
  List<QLContainerHistogramData> containerHistogramDataList;
}
