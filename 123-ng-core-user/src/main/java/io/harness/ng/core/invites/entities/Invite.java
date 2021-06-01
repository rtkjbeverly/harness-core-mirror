package io.harness.ng.core.invites.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.invites.remote.RoleBinding;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import com.mongodb.lang.NonNull;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder(toBuilder = true)
@FieldNameConstants(innerTypeName = "InviteKeys")
@Entity(value = "invites", noClassnameStored = true)
@Document("invites")
@TypeAlias("invites")
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(PL)
public class Invite implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("ng_invite_account_org_project_identifiers_email_role_deleted")
                 .field(InviteKeys.deleted)
                 .field(InviteKeys.email)
                 .field(InviteKeys.accountIdentifier)
                 .field(InviteKeys.orgIdentifier)
                 .field(InviteKeys.projectIdentifier)
                 .build())
        .build();
  }

  @NotEmpty String accountIdentifier;
  @Id @org.mongodb.morphia.annotations.Id @EntityIdentifier String id;
  String orgIdentifier;
  String projectIdentifier;
  @NotEmpty String email;
  @Size(min = 1, max = 100) List<RoleBinding> roleBindings;
  String name;
  @NonNull InviteType inviteType;
  @CreatedDate Long createdAt;
  @Version Long version;
  String inviteToken;
  @NonNull @Builder.Default Boolean approved = Boolean.FALSE;
  @Builder.Default Boolean deleted = Boolean.FALSE;

  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  @Builder.Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(30).toInstant());

  @OwnedBy(PL)
  public enum InviteType {
    @JsonProperty("USER_INITIATED_INVITE") USER_INITIATED_INVITE("USER_INITIATED_INVITE"),
    @JsonProperty("ADMIN_INITIATED_INVITE") ADMIN_INITIATED_INVITE("ADMIN_INITIATED_INVITE");

    private final String type;
    InviteType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }
}