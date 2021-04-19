package io.harness.accesscontrol.acl.models;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.accesscontrol.acl.models.SourceMetadata.SourceMetadataKeys;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ACLKeys")
@Document("acl")
@Entity(value = "acl", noClassnameStored = true)
@TypeAlias("acl")
@StoreIn(ACCESS_CONTROL)
public class ACL implements PersistentEntity {
  private static final String DELIMITER = "$";
  public static final String ROLE_ASSIGNMENT_IDENTIFIER_KEY =
      ACLKeys.sourceMetadata + "." + SourceMetadataKeys.roleAssignmentIdentifier;
  public static final String USER_GROUP_IDENTIFIER_KEY =
      ACLKeys.sourceMetadata + "." + SourceMetadataKeys.userGroupIdentifier;
  public static final String ROLE_IDENTIFIER_KEY = ACLKeys.sourceMetadata + "." + SourceMetadataKeys.roleIdentifier;
  public static final String RESOURCE_GROUP_IDENTIFIER_KEY =
      ACLKeys.sourceMetadata + "." + SourceMetadataKeys.resourceGroupIdentifier;

  @Id @org.mongodb.morphia.annotations.Id private String id;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  @FdIndex String roleAssignmentId;
  String scopeIdentifier;
  String permissionIdentifier;
  SourceMetadata sourceMetadata;
  String resourceSelector;
  String principalType;
  String principalIdentifier;
  String aclQueryString;
  @FdIndex @Getter(value = AccessLevel.NONE) private Boolean enabled;

  public boolean isEnabled() {
    return this.enabled != null && enabled;
  }

  @Value
  public static class RoleAssignmentPermissionPrincipal {
    String scopeIdentifier;
    String roleAssignmentIdentifier;
    String permissionIdentifier;
    String principalIdentifier;
    String principalType;
  }

  @Value
  public static class RoleAssignmentResourceSelectorPrincipal {
    String scopeIdentifier;
    String roleAssignmentIdentifier;
    String resourceSelector;
    String principalIdentifier;
    String principalType;
  }

  @Value
  public static class RoleAssignmentResourceSelectorPermission {
    String scopeIdentifier;
    String roleAssignmentIdentifier;
    String resourceSelector;
    String permissionIdentifier;
  }

  public RoleAssignmentPermissionPrincipal roleAssignmentPermissionPrincipal() {
    return new RoleAssignmentPermissionPrincipal(this.scopeIdentifier,
        this.sourceMetadata.getRoleAssignmentIdentifier(), this.permissionIdentifier, this.principalIdentifier,
        this.principalType);
  }

  public RoleAssignmentResourceSelectorPrincipal roleAssignmentResourceSelectorPrincipal() {
    return new RoleAssignmentResourceSelectorPrincipal(this.scopeIdentifier,
        this.sourceMetadata.getRoleAssignmentIdentifier(), this.resourceSelector, this.principalIdentifier,
        this.principalType);
  }

  public RoleAssignmentResourceSelectorPermission roleAssignmentResourceSelectorPermission() {
    return new RoleAssignmentResourceSelectorPermission(this.scopeIdentifier,
        this.sourceMetadata.getRoleAssignmentIdentifier(), this.resourceSelector, this.permissionIdentifier);
  }

  public static ACL copyOf(ACL acl) {
    return ACL.builder()
        .roleAssignmentId(acl.getRoleAssignmentId())
        .scopeIdentifier(acl.getScopeIdentifier())
        .permissionIdentifier(acl.getPermissionIdentifier())
        .sourceMetadata(acl.getSourceMetadata())
        .resourceSelector(acl.getResourceSelector())
        .principalType(acl.getPrincipalType())
        .principalIdentifier(acl.getPrincipalIdentifier())
        .aclQueryString(acl.getAclQueryString())
        .build();
  }

  public static String getAclQueryString(String scopeIdentifier, String resourceSelector, String principalType,
      String principalIdentifier, String permissionIdentifier) {
    return scopeIdentifier + DELIMITER + permissionIdentifier + DELIMITER + resourceSelector + DELIMITER + principalType
        + DELIMITER + principalIdentifier;
  }

  public static String getAclQueryString(@NotNull ACL acl) {
    return getAclQueryString(acl.getScopeIdentifier(), acl.getResourceSelector(), acl.getPrincipalType(),
        acl.getPrincipalIdentifier(), acl.getPermissionIdentifier());
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("uniqueIdx")
                 .field(ACLKeys.scopeIdentifier)
                 .field(ROLE_ASSIGNMENT_IDENTIFIER_KEY)
                 .field(USER_GROUP_IDENTIFIER_KEY)
                 .field(ROLE_IDENTIFIER_KEY)
                 .field(RESOURCE_GROUP_IDENTIFIER_KEY)
                 .field(ACLKeys.resourceSelector)
                 .field(ACLKeys.permissionIdentifier)
                 .field(ACLKeys.principalIdentifier)
                 .field(ACLKeys.principalType)
                 .unique(true)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("roleIdx")
                 .field(SourceMetadataKeys.roleIdentifier)
                 .field(ACLKeys.scopeIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("resourceGroupIdx")
                 .field(SourceMetadataKeys.resourceGroupIdentifier)
                 .field(ACLKeys.scopeIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("aclQueryStringEnabledIdx")
                 .field(ACLKeys.aclQueryString)
                 .field(ACLKeys.enabled)
                 .build())
        .build();
  }
}