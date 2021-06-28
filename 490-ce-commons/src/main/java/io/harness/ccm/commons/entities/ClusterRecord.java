package io.harness.ccm.commons.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ClusterRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "clusterRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
@OwnedBy(CE)
@StoreIn("events")
public class ClusterRecord implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_k8sBaseConnectorRefIdentifier")
                 .field(ClusterRecordKeys.accountId)
                 .field(ClusterRecordKeys.k8sBaseConnectorRefIdentifier)
                 .unique(true)
                 .build())
        .build();
  }
  @Id String uuid;
  String accountId;
  String orgIdentifier; // Added For Future Scope
  String projectIdentifier; // Added For Future Scope
  String ceK8sConnectorIdentifier;
  String k8sBaseConnectorRefIdentifier;
  String clusterName;
  String perpetualTaskId;
  boolean isDeactivated;
  @SchemaIgnore long createdAt;
  long lastUpdatedAt;
}