package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLInfrastructureDefinitionTagFilter implements EntityFilter {
  private QLInfrastructureDefinitionTagType entityType;
  private List<QLTagInput> tags;
}