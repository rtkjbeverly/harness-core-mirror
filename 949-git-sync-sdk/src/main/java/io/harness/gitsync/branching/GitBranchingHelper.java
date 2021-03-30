package io.harness.gitsync.branching;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.branching.EntityGitBranchMetadata.EntityGitBranchMetadataKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class GitBranchingHelper {
  // Adding mongo template here instead of repository as I dont want consumers to add repository in spring config.
  private final MongoTemplate mongoTemplate;

  public List<String> getObjectIdForYamlGitConfigBranchAndScope(String yamlGitConfigId, String branch,
      String projectIdentifier, String orgIdentifier, String accountId, EntityType entityType) {
    Criteria criteria = Criteria.where(EntityGitBranchMetadataKeys.accountId)
                            .is(accountId)
                            .and(EntityGitBranchMetadataKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(EntityGitBranchMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(EntityGitBranchMetadataKeys.entityType)
                            .is(entityType.getYamlName())
                            .and(EntityGitBranchMetadataKeys.yamlGitConfigId)
                            .is(yamlGitConfigId)
                            .and(EntityGitBranchMetadataKeys.branch)
                            .is(branch);
    final List<EntityGitBranchMetadata> entityGitBranchMetadata =
        mongoTemplate.find(query(criteria), EntityGitBranchMetadata.class);
    return entityGitBranchMetadata.stream().map(EntityGitBranchMetadata::getBranch).collect(Collectors.toList());
  }

  public List<String> getObjectIdForDefaultBranchAndScope(String projectIdentifier,
      String

          orgIdentifier,
      String accountId, EntityType entityType) {
    Criteria criteria = Criteria.where(EntityGitBranchMetadataKeys.accountId)
                            .is(accountId)
                            .and(EntityGitBranchMetadataKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(EntityGitBranchMetadataKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(EntityGitBranchMetadataKeys.entityType)
                            .is(entityType.getYamlName())
                            .and(EntityGitBranchMetadataKeys.isDefault)
                            .is(true);
    final List<EntityGitBranchMetadata> entityGitBranchMetadata =
        mongoTemplate.find(query(criteria), EntityGitBranchMetadata.class);
    return entityGitBranchMetadata.stream().map(EntityGitBranchMetadata::getBranch).collect(Collectors.toList());
  }

  public List<String> getObjectIdForYamlGitConfigBranchAndFqn(
      String yamlGitConfigId, String branch, String fqn, EntityType entityType) {
    Criteria criteria = Criteria.where(EntityGitBranchMetadataKeys.entityFqn)
                            .is(fqn)
                            .and(EntityGitBranchMetadataKeys.entityType)
                            .is(entityType.getYamlName())
                            .and(EntityGitBranchMetadataKeys.yamlGitConfigId)
                            .is(yamlGitConfigId)
                            .and(EntityGitBranchMetadataKeys.branch)
                            .is(branch);
    final List<EntityGitBranchMetadata> entityGitBranchMetadata =
        mongoTemplate.find(query(criteria), EntityGitBranchMetadata.class);
    return entityGitBranchMetadata.stream().map(EntityGitBranchMetadata::getBranch).collect(Collectors.toList());
  }

  public List<String> getObjectIdForDefaultBranchAndFqn(String fqn, EntityType entityType) {
    Criteria criteria = Criteria.where(EntityGitBranchMetadataKeys.entityFqn)
                            .is(fqn)
                            .and(EntityGitBranchMetadataKeys.entityType)
                            .is(entityType.getYamlName())
                            .and(EntityGitBranchMetadataKeys.isDefault)
                            .is(true);
    final List<EntityGitBranchMetadata> entityGitBranchMetadata =
        mongoTemplate.find(query(criteria), EntityGitBranchMetadata.class);
    return entityGitBranchMetadata.stream().map(EntityGitBranchMetadata::getBranch).collect(Collectors.toList());
  }

  public boolean save(EntityGitBranchMetadata entityGitBranchMetadata) {
    mongoTemplate.save(entityGitBranchMetadata);
    return true;
  }
}