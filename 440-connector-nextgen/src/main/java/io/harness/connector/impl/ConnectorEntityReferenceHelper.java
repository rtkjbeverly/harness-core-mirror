package io.harness.connector.impl;

import static io.harness.NGConstants.ENTITY_REFERENCE_LOG_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.encryption.SecretRefData;
import io.harness.entitysetupusageclient.EntitySetupUsageHelper;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ConnectorEntityReferenceHelper {
  EntitySetupUsageHelper entityReferenceHelper;
  EntitySetupUsageClient entitySetupUsageClient;
  Producer eventProducer;
  IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  SecretRefInputValidationHelper secretRefInputValidationHelper;

  private final String deleteLogMsg =
      "The entity reference was not deleted when the connector [{}] was deleted that used secret [{}] with the exception[{}]";
  private final String createFailedLogMsg =
      "The entity reference was not created for the connector [{}] using the secret [{}] with the exception[{}]";
  private final String updateFailedLogMsg =
      "The entity reference was not updated for the connector [{}] using the secret [{}] with the exception[{}]";

  @Inject
  public ConnectorEntityReferenceHelper(EntitySetupUsageHelper entityReferenceHelper,
      EntitySetupUsageClient entitySetupUsageClient,
      @Named(EventsFrameworkConstants.SETUP_USAGE) Producer eventProducer,
      IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper,
      SecretRefInputValidationHelper secretRefInputValidationHelper) {
    this.entityReferenceHelper = entityReferenceHelper;
    this.entitySetupUsageClient = entitySetupUsageClient;
    this.eventProducer = eventProducer;
    this.secretRefInputValidationHelper = secretRefInputValidationHelper;
    this.identifierRefProtoDTOHelper = identifierRefProtoDTOHelper;
  }

  public boolean createSetupUsageForSecret(
      ConnectorInfoDTO connectorInfoDTO, String accountIdentifier, boolean isUpdate) {
    String logMessage = isUpdate ? updateFailedLogMsg : createFailedLogMsg;
    String connectorFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorInfoDTO.getOrgIdentifier(), connectorInfoDTO.getProjectIdentifier(), connectorInfoDTO.getIdentifier());
    List<DecryptableEntity> decryptableEntities = connectorInfoDTO.getConnectorConfig().getDecryptableEntities();
    if (isEmpty(decryptableEntities)) {
      return produceEventForSetupUsage(
          connectorInfoDTO, new ArrayList<>(), accountIdentifier, connectorFQN, new ArrayList<>(), logMessage);
    }
    Set<SecretRefData> secrets = secretRefInputValidationHelper.getDecryptableFieldsData(decryptableEntities);

    NGAccess baseNGAccess = buildBaseNGAccess(connectorInfoDTO, accountIdentifier);
    List<String> secretFQNs = getAllSecretFQNs(secrets, baseNGAccess);
    List<EntityDetailProtoDTO> allSecretDetails = getAllReferredSecretDetails(secrets, baseNGAccess);

    log.info(ENTITY_REFERENCE_LOG_PREFIX
            + "[{}] the entity reference when the connector [{}] was created using the secret [{}]",
        isUpdate ? "Updating" : "Creating", connectorFQN, secretFQNs);

    return produceEventForSetupUsage(
        connectorInfoDTO, allSecretDetails, accountIdentifier, connectorFQN, secretFQNs, logMessage);
  }

  public boolean deleteConnectorEntityReferenceWhenConnectorGetsDeleted(
      ConnectorInfoDTO connectorInfoDTO, String accountIdentifier) {
    String connectorFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorInfoDTO.getOrgIdentifier(), connectorInfoDTO.getProjectIdentifier(), connectorInfoDTO.getIdentifier());

    List<DecryptableEntity> decryptableEntities = connectorInfoDTO.getConnectorConfig().getDecryptableEntities();
    if (isEmpty(decryptableEntities)) {
      return true;
    }
    Set<SecretRefData> secrets = secretRefInputValidationHelper.getDecryptableFieldsData(decryptableEntities);

    NGAccess baseNGAccess = buildBaseNGAccess(connectorInfoDTO, accountIdentifier);
    List<String> secretFQNs = getAllSecretFQNs(secrets, baseNGAccess);
    List<EntityDetailProtoDTO> allSecretDetails = new ArrayList<>();
    return produceEventForSetupUsage(
        connectorInfoDTO, allSecretDetails, accountIdentifier, connectorFQN, secretFQNs, deleteLogMsg);
  }
  private boolean produceEventForSetupUsage(ConnectorInfoDTO connectorInfoDTO,
      List<EntityDetailProtoDTO> allSecretDetails, String accountIdentifier, String connectorFQN,
      List<String> secretFQNs, String logMessage) {
    EntitySetupUsageCreateV2DTO entityReferenceDTO =
        createSetupUsageDTOForConnector(connectorInfoDTO, allSecretDetails, accountIdentifier);
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                  EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, EntityTypeProtoEnum.SECRETS.name(),
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
      return true;
    } catch (Exception ex) {
      log.info(ENTITY_REFERENCE_LOG_PREFIX + logMessage, connectorFQN, secretFQNs, ex.getMessage());
      return false;
    }
  }
  private EntitySetupUsageCreateV2DTO createSetupUsageDTOForConnector(
      ConnectorInfoDTO connectorInfoDTO, List<EntityDetailProtoDTO> secretDetails, String accountIdentifier) {
    IdentifierRefProtoDTO connectorReference =
        identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(accountIdentifier, connectorInfoDTO.getOrgIdentifier(),
            connectorInfoDTO.getProjectIdentifier(), connectorInfoDTO.getIdentifier());
    EntityDetailProtoDTO connectorDetails = EntityDetailProtoDTO.newBuilder()
                                                .setIdentifierRef(connectorReference)
                                                .setType(EntityTypeProtoEnum.CONNECTORS)
                                                .setName(connectorInfoDTO.getName())
                                                .build();

    return EntitySetupUsageCreateV2DTO.newBuilder()
        .setAccountIdentifier(accountIdentifier)
        .setReferredByEntity(connectorDetails)
        .addAllReferredEntities(secretDetails)
        .setDeleteOldReferredByRecords(true)
        .build();
  }

  private List<IdentifierRef> getAllSecretIdentifiers(Set<SecretRefData> secrets, NGAccess baseNGAccess) {
    List<IdentifierRef> secretIdentifierRef = new ArrayList<>();
    for (SecretRefData secret : secrets) {
      if (secret != null && !secret.isNull()) {
        secretIdentifierRef.add(IdentifierRefHelper.getIdentifierRef(secret.toSecretRefStringValue(),
            baseNGAccess.getAccountIdentifier(), baseNGAccess.getOrgIdentifier(), baseNGAccess.getProjectIdentifier()));
      }
    }
    return secretIdentifierRef;
  }

  private NGAccess buildBaseNGAccess(ConnectorInfoDTO connectorInfoDTO, String accountIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connectorInfoDTO.getOrgIdentifier())
        .projectIdentifier(connectorInfoDTO.getProjectIdentifier())
        .build();
  }

  private List<String> getAllSecretFQNs(Set<SecretRefData> secrets, NGAccess baseNGAccess) {
    List<IdentifierRef> secretIdentifiers = getAllSecretIdentifiers(secrets, baseNGAccess);
    List<String> secretFQNs = new ArrayList<>();
    for (IdentifierRef secretIdentifier : secretIdentifiers) {
      if (secretIdentifier != null) {
        secretFQNs.add(FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(baseNGAccess.getAccountIdentifier(),
            secretIdentifier.getOrgIdentifier(), secretIdentifier.getProjectIdentifier(),
            secretIdentifier.getIdentifier()));
      }
    }
    return secretFQNs;
  }

  private List<EntityDetailProtoDTO> getAllReferredSecretDetails(Set<SecretRefData> secrets, NGAccess baseNGAccess) {
    List<IdentifierRef> secretIdentifiers = getAllSecretIdentifiers(secrets, baseNGAccess);
    List<EntityDetailProtoDTO> allSecretDetails = new ArrayList<>();
    for (IdentifierRef secretIdentifierRef : secretIdentifiers) {
      if (secretIdentifierRef != null) {
        IdentifierRefProtoDTO secretReference = identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(
            baseNGAccess.getAccountIdentifier(), secretIdentifierRef.getOrgIdentifier(),
            secretIdentifierRef.getProjectIdentifier(), secretIdentifierRef.getIdentifier());

        allSecretDetails.add(EntityDetailProtoDTO.newBuilder()
                                 .setIdentifierRef(secretReference)
                                 .setType(EntityTypeProtoEnum.SECRETS)
                                 .build());
      }
    }
    return allSecretDetails;
  }
}