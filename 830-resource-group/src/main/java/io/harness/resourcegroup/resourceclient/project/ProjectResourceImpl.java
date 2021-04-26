package io.harness.resourcegroup.resourceclient.project;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.project.remote.ProjectClient;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.beans.ResourceGroupConstants;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class ProjectResourceImpl implements Resource {
  ProjectClient projectClient;

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    if (isEmpty(resourceIds)) {
      return Collections.emptyList();
    }
    PageResponse<ProjectResponse> projects =
        getResponse(projectClient.listProjects(scope.getAccountIdentifier(), scope.getOrgIdentifier(), resourceIds));
    Set<String> validResourceIds =
        projects.getContent().stream().map(e -> e.getProject().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream()
        .map(resourceId -> validResourceIds.contains(resourceId) && scope.getProjectIdentifier().equals(resourceId))
        .collect(toList());
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(STATIC);
  }

  @Override
  public String getType() {
    return ResourceGroupConstants.PROJECT;
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return Collections.emptySet();
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.PROJECT_ENTITY);
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO = null;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking ProjectEntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(projectEntityChangeDTO)) {
      return null;
    }
    return ResourceInfo.builder()
        .accountIdentifier(projectEntityChangeDTO.getAccountIdentifier())
        .orgIdentifier(projectEntityChangeDTO.getOrgIdentifier())
        .projectIdentifier(projectEntityChangeDTO.getIdentifier())
        .resourceType(getType())
        .resourceIdentifier(projectEntityChangeDTO.getIdentifier())
        .build();
  }
}