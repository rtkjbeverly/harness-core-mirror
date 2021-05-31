package io.harness.ccm.commons.entities.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.NodePoolId.NodePoolIdKeys;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.beans.recommendation.models.RecommendationResponse;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.persistence.ValidUntilAccess;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "K8sNodeRecommendationKeys")
@StoreIn("events")
@Entity(value = "k8sNodeRecommendation", noClassnameStored = true)
@OwnedBy(CE)
public final class K8sNodeRecommendation
    implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware, ValidUntilAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_clusterid_nodepoolname")
                 .unique(true)
                 .field(K8sNodeRecommendationKeys.accountId)
                 .field(K8sNodeRecommendationKeys.nodePoolId + "." + NodePoolIdKeys.clusterid)
                 // It's okay that nodepoolname is nullable, right?
                 .field(K8sNodeRecommendationKeys.nodePoolId + "." + NodePoolIdKeys.nodepoolname)
                 .build())
        .build();
  }

  @Id String uuid;
  String accountId;
  NodePoolId nodePoolId;

  TotalResourceUsage totalResourceUsage;

  RecommendationResponse recommendation;

  long createdAt;
  long lastUpdatedAt;

  @EqualsAndHashCode.Exclude
  @Builder.Default
  @FdTtlIndex
  Date validUntil = Date.from(OffsetDateTime.now().plusDays(90).toInstant());

  @Builder.Default int version = 1;
}