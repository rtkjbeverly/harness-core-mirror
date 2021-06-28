package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.core.beans.monitoredService.HealthSource.CVConfigUpdateResult;
import io.harness.cvng.core.entities.CVConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = AppDynamicsHealthSourceSpec.class, name = "AppDynamics") })
public abstract class HealthSourceSpec {
  public abstract CVConfigUpdateResult getCVConfigUpdateResult(String accountId, String orgIdentifier,
      String projectIdentifier, String environmentRef, String serviceRef, String identifier, String name,
      List<CVConfig> existingCVConfigs);
  public abstract String getConnectorRef();

  protected void fillCommonFields(CVConfig cvConfig, String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, String connectorRef, String name, String feature) {
    cvConfig.setAccountId(accountId);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier(identifier);
    cvConfig.setConnectorIdentifier(connectorRef);
    cvConfig.setMonitoringSourceName(name);
    cvConfig.setProductName(feature);
  }
}