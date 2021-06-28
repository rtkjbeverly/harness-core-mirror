package io.harness.accesscontrol.principals.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDao;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Slf4j
@Singleton
@ValidateOnExecution
public class ServiceAccountServiceImpl implements ServiceAccountService {
  private final ServiceAccountDao serviceAccountDao;
  private final RoleAssignmentService roleAssignmentService;
  private final TransactionTemplate transactionTemplate;
  private static final RetryPolicy<Object> deleteServiceAccountTransactionPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed to delete service account and corresponding role assignments; attempt: {}",
      "[Failed]: Failed to delete service account and corresponding role assignments; attempt: {}",
      ImmutableList.of(TransactionException.class), Duration.ofSeconds(5), 3, log);

  @Inject
  public ServiceAccountServiceImpl(ServiceAccountDao serviceAccountDao, TransactionTemplate transactionTemplate,
      RoleAssignmentService roleAssignmentService) {
    this.serviceAccountDao = serviceAccountDao;
    this.transactionTemplate = transactionTemplate;
    this.roleAssignmentService = roleAssignmentService;
  }

  @Override
  public ServiceAccount createIfNotPresent(ServiceAccount serviceAccount) {
    return serviceAccountDao.createIfNotPresent(serviceAccount);
  }

  @Override
  public PageResponse<ServiceAccount> list(PageRequest pageRequest, String scopeIdentifier) {
    return serviceAccountDao.list(pageRequest, scopeIdentifier);
  }

  @Override
  public Optional<ServiceAccount> get(String identifier, String scopeIdentifier) {
    return serviceAccountDao.get(identifier, scopeIdentifier);
  }

  @Override
  public Optional<ServiceAccount> deleteIfPresent(String identifier, String scopeIdentifier) {
    Optional<ServiceAccount> optionalServiceAccount = get(identifier, scopeIdentifier);
    if (optionalServiceAccount.isPresent()) {
      return Optional.of(deleteInternal(identifier, scopeIdentifier));
    }
    return Optional.empty();
  }

  private ServiceAccount deleteInternal(String identifier, String scopeIdentifier) {
    return Failsafe.with(deleteServiceAccountTransactionPolicy).get(() -> transactionTemplate.execute(status -> {
      deleteServiceAccountRoleAssignments(identifier, scopeIdentifier);
      return serviceAccountDao.delete(identifier, scopeIdentifier)
          .orElseThrow(
              ()
                  -> new UnexpectedException(String.format(
                      "Failed to delete the service account %s in the scope %s", identifier, scopeIdentifier)));
    }));
  }

  private long deleteServiceAccountRoleAssignments(String identifier, String scopeIdentifier) {
    return roleAssignmentService.deleteMulti(
        RoleAssignmentFilter.builder()
            .scopeFilter(scopeIdentifier)
            .principalFilter(Sets.newHashSet(Principal.builder()
                                                 .principalType(PrincipalType.SERVICE_ACCOUNT)
                                                 .principalIdentifier(identifier)
                                                 .build()))
            .build());
  }
}