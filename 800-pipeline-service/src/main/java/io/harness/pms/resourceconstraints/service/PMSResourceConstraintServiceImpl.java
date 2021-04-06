package io.harness.pms.resourceconstraints.service;

import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceConstraint;
import io.harness.beans.shared.RestraintService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;
import io.harness.pms.utils.PmsConstants;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class PMSResourceConstraintServiceImpl implements PMSResourceConstraintService {
  public static final String NOT_FOUND_WITH_ARGUMENTS = "Resource Constraint not found for accountId : %s";

  private final RestraintService restraintService;
  private final ResourceRestraintService resourceRestraintService;
  private final PlanExecutionService planExecutionService;

  public List<ResourceConstraintExecutionInfoDTO> getResourceConstraintExecutionInfoList(
      String accountId, String resourceUnit) {
    ResourceConstraint resourceConstraint =
        restraintService.getByNameAndAccountId(PmsConstants.QUEUING_RC_NAME, accountId);
    if (resourceConstraint == null) {
      throw new InvalidRequestException(String.format(NOT_FOUND_WITH_ARGUMENTS, accountId));
    }

    List<ResourceRestraintInstance> instances = resourceRestraintService.getAllByRestraintIdAndResourceUnitAndStates(
        resourceConstraint.getUuid(), resourceUnit, Arrays.asList(ACTIVE, BLOCKED));
    instances.sort(Comparator.comparingInt(ResourceRestraintInstance::getOrder));

    Map<String, PlanExecution> planExecutionMap =
        planExecutionService
            .findAllByPlanExecutionIdIn(
                instances.stream().map(ResourceRestraintInstance::getReleaseEntityId).collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(PlanExecution::getUuid, Function.identity()));

    return instances.stream()
        .map(instance
            -> ResourceConstraintExecutionInfoDTO.builder()
                   .pipelineIdentifier(
                       planExecutionMap.get(instance.getReleaseEntityId()).getMetadata().getPipelineIdentifier())
                   .planExecutionId(instance.getReleaseEntityId())
                   .state(instance.getState())
                   .build())
        .collect(Collectors.toList());
  }
}
