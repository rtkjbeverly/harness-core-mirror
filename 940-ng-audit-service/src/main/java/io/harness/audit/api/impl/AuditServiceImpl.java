package io.harness.audit.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.mapper.AuditEventMapper.fromDTO;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditFilterPropertiesDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.audit.entities.AuditEvent.AuditEventKeys;
import io.harness.audit.repositories.AuditRepository;
import io.harness.ng.beans.PageRequest;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditServiceImpl implements AuditService {
  private final AuditRepository auditRepository;

  @Override
  public AuditEvent create(AuditEventDTO auditEventDTO) {
    AuditEvent auditEvent = fromDTO(auditEventDTO);
    return auditRepository.save(auditEvent);
  }

  @Override
  public Page<AuditEvent> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      PageRequest pageRequest, AuditFilterPropertiesDTO auditFilterPropertiesDTO) {
    Criteria criteria = Criteria.where(AuditEventKeys.ACCOUNT_IDENTIFIER_KEY)
                            .is(accountIdentifier)
                            .and(AuditEventKeys.ORG_IDENTIFIER_KEY)
                            .is(orgIdentifier)
                            .and(AuditEventKeys.PROJECT_IDENTIFIER_KEY)
                            .is(projectIdentifier);
    return auditRepository.findAll(criteria, getPageRequest(pageRequest));
  }
}