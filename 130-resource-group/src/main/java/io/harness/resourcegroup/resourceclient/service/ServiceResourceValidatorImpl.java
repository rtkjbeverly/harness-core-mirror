package io.harness.resourcegroup.resourceclient.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.ng.core.service.dto.ServiceResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.ResourcePrimaryKey;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.model.Scope;
import io.harness.service.remote.ServiceResourceClient;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
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
@OwnedBy(PIPELINE)
public class ServiceResourceValidatorImpl implements ResourceValidator {
  @Inject ServiceResourceClient serviceResourceClient;
  @Override
  public String getResourceType() {
    return "SERVICE";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.SERVICE_ENTITY);
  }

  @Override
  public ResourcePrimaryKey getResourceGroupKeyFromEvent(Message message) {
    EntityChangeDTO entityChangeDTO = null;
    try {
      entityChangeDTO = EntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking EntityChangeDTO for key {}", message.getId(), e);
    }
    if (Objects.isNull(entityChangeDTO)) {
      return null;
    }
    return ResourcePrimaryKey.builder()
        .accountIdentifier(entityChangeDTO.getAccountIdentifier().getValue())
        .orgIdentifier(entityChangeDTO.getOrgIdentifier().getValue())
        .projectIdentifer(entityChangeDTO.getProjectIdentifier().getValue())
        .resourceType(getResourceType())
        .resourceIdetifier(entityChangeDTO.getIdentifier().getValue())
        .build();
  }

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<ServiceResponse> serviceResponses =
        NGRestUtils
            .getResponse(serviceResourceClient.listServicesForProject(
                0, resourceIds.size(), accountIdentifier, orgIdentifier, projectIdentifier, resourceIds, null))
            .getContent();
    Set<String> validResourceId =
        serviceResponses.stream().map(e -> e.getService().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourceId::contains).collect(Collectors.toList());
  }

  @Override
  public EnumSet<ValidatorType> getValidatorTypes() {
    return EnumSet.of(STATIC, DYNAMIC);
  }
}