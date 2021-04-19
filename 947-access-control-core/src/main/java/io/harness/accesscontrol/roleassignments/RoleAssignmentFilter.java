package io.harness.accesscontrol.roleassignments;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.validator.ValidRoleAssignmentFilter;
import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ValidRoleAssignmentFilter
@OwnedBy(PL)
public class RoleAssignmentFilter {
  @NotEmpty String scopeFilter;
  boolean includeChildScopes;
  @Builder.Default @NotNull @Size(max = 100) Set<String> resourceGroupFilter = new HashSet<>();
  @Builder.Default @NotNull @Size(max = 100) Set<String> roleFilter = new HashSet<>();
  @Builder.Default @NotNull @Size(max = 100) Set<PrincipalType> principalTypeFilter = new HashSet<>();
  @Builder.Default @NotNull @Size(max = 100) Set<Principal> principalFilter = new HashSet<>();
  @Builder.Default @NotNull @Valid ManagedFilter managedFilter = NO_FILTER;
  @Builder.Default @NotNull @Size(max = 100) Set<Boolean> disabledFilter = new HashSet<>();
}