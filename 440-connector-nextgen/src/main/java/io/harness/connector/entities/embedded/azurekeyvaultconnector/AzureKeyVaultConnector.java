package io.harness.connector.entities.embedded.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.azure.AzureEnvironmentType.AZURE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.connector.entities.Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "VaultConnectorKeys")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.azurekeyvaultconnector.AzureKeyVaultConnector")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureKeyVaultConnector extends Connector {
  String clientId;
  String tenantId;
  String vaultName;
  String subscription;
  boolean isDefault;

  @Builder.Default AzureEnvironmentType azureEnvironmentType = AZURE;
}