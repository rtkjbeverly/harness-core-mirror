package io.harness.filters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class ConnectorRefExtractorHelper implements EntityReferenceExtractor {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return null;
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    if (!(object instanceof WithConnectorRef)) {
      throw new InvalidRequestException(String.format(
          "Object of class %s does not implement WithConnectorRef, and hence can't have ConnectorRefExtractorHelper as its visitor helper",
          object.getClass().toString()));
    }
    WithConnectorRef withConnectorRef = (WithConnectorRef) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    for (String key : withConnectorRef.extractConnectorRefs().keySet()) {
      ParameterField<String> connectorRef = withConnectorRef.extractConnectorRefs().get(key);

      if (ParameterField.isNull(connectorRef)) {
        return result;
      }
      String fullQualifiedDomainName =
          VisitorParentPathUtils.getFullQualifiedDomainName(contextMap) + PATH_CONNECTOR + key;
      result.add(FilterCreatorHelper.convertToEntityDetailProtoDTO(accountIdentifier, orgIdentifier, projectIdentifier,
          fullQualifiedDomainName, connectorRef, EntityTypeProtoEnum.CONNECTORS));
    }
    return result;
  }
}
