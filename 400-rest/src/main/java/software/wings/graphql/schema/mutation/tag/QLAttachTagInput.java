package software.wings.graphql.schema.mutation.tag;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAttachTagInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAttachTagInput implements QLMutationInput {
  private String clientMutationId;
  private String entityId;
  private String name;
  private String value;
  private QLEntityType entityType;
}