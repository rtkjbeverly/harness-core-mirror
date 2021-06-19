package io.harness.accesscontrol.principals.users.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface UserRepository extends PagingAndSortingRepository<UserDBO, String>, UserRepositoryCustom {
  Optional<UserDBO> findByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);

  Page<UserDBO> findByScopeIdentifier(String scopeIdentifier, Pageable pageable);

  List<UserDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String scopeIdentifier);
}