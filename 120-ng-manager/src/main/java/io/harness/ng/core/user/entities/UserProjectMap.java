package io.harness.ng.core.user.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.invites.entities.Role;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "UserProjectMapKeys")
@Entity(value = "userProjectMaps", noClassnameStored = true)
@Document("userProjectMaps")
@TypeAlias("userProjectMaps")
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(PL)
public class UserProjectMap implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .unique(true)
                 .name("uniqueUserProjectMap")
                 .field(UserProjectMapKeys.userId)
                 .field(UserProjectMapKeys.accountIdentifier)
                 .field(UserProjectMapKeys.orgIdentifier)
                 .field(UserProjectMapKeys.projectIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .unique(false)
                 .name("accountOrgProjectMapIdx")
                 .field(UserProjectMapKeys.accountIdentifier)
                 .field(UserProjectMapKeys.orgIdentifier)
                 .field(UserProjectMapKeys.projectIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder().name("userProjectMapMigrationIdx").field(UserProjectMapKeys.migrated).build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @Trimmed @NotEmpty String userId;
  @Trimmed @NotEmpty String accountIdentifier;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty List<Role> roles;
  Boolean migrated;
  @Wither @Version Long version;
}