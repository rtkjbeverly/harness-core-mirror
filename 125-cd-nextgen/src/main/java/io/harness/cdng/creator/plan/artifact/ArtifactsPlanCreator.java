package io.harness.cdng.creator.plan.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.steps.ArtifactStepParameters.ArtifactStepParametersBuilder;
import io.harness.cdng.artifact.steps.ArtifactsStep;
import io.harness.cdng.artifact.steps.SidecarsStep;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.facilitator.chilidren.ChildrenFacilitator;
import io.harness.pms.sdk.core.facilitator.task.TaskFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.fork.ForkStepParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.ImmutablePair;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class ArtifactsPlanCreator {
  public PlanCreationResponse createPlanForArtifactsNode(ServiceConfig serviceConfig) {
    ArtifactListConfig artifactListConfig = serviceConfig.getServiceDefinition().getServiceSpec().getArtifacts();
    ArtifactListBuilder artifactListBuilder = new ArtifactListBuilder(artifactListConfig);
    artifactListBuilder.addOverrideSets(serviceConfig);
    artifactListBuilder.addStageOverrides(serviceConfig);
    ArtifactList artifactList = artifactListBuilder.build();
    if (artifactList.getPrimary() == null && EmptyPredicate.isEmpty(artifactList.getSidecars())) {
      return PlanCreationResponse.builder().build();
    }

    List<PlanNode> planNodes = new ArrayList<>();
    List<String> childrenIds = new ArrayList<>();
    if (artifactList.getPrimary() != null) {
      PlanNode primaryNode = createPlanForArtifactNode(YamlTypes.PRIMARY_ARTIFACT, artifactList.getPrimary());
      planNodes.add(primaryNode);
      childrenIds.add(primaryNode.getUuid());
    }
    if (EmptyPredicate.isNotEmpty(artifactList.getSidecars())) {
      ImmutablePair<String, List<PlanNode>> pair =
          createPlanForSidecarsNode(artifactListConfig.getUuid(), artifactList);
      planNodes.addAll(pair.getRight());
      childrenIds.add(pair.getLeft());
    }

    ForkStepParameters stepParameters = ForkStepParameters.builder().parallelNodeIds(childrenIds).build();
    PlanNode artifactsNode =
        PlanNode.builder()
            .uuid(artifactListConfig.getUuid())
            .stepType(ArtifactsStep.STEP_TYPE)
            .name(PlanCreatorConstants.ARTIFACTS_NODE_NAME)
            .identifier(YamlTypes.ARTIFACT_LIST_CONFIG)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder().setType(ChildrenFacilitator.FACILITATOR_TYPE).build())
            .skipExpressionChain(false)
            .build();
    planNodes.add(artifactsNode);
    return PlanCreationResponse.builder()
        .nodes(planNodes.stream().collect(Collectors.toMap(PlanNode::getUuid, Function.identity())))
        .startingNodeId(artifactsNode.getUuid())
        .build();
  }

  private ImmutablePair<String, List<PlanNode>> createPlanForSidecarsNode(
      String artifactListUuid, ArtifactList artifactList) {
    List<PlanNode> planNodes = new ArrayList<>();
    for (Map.Entry<String, ArtifactInfo> entry : artifactList.getSidecars().entrySet()) {
      planNodes.add(createPlanForArtifactNode(entry.getKey(), entry.getValue()));
    }

    ForkStepParameters stepParameters =
        ForkStepParameters.builder()
            .parallelNodeIds(planNodes.stream().map(PlanNode::getUuid).collect(Collectors.toList()))
            .build();
    PlanNode sidecarsNode =
        PlanNode.builder()
            .uuid("sidecars-" + artifactListUuid)
            .stepType(SidecarsStep.STEP_TYPE)
            .name(PlanCreatorConstants.SIDECARS_NODE_NAME)
            .identifier(YamlTypes.SIDECARS_ARTIFACT_CONFIG)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder().setType(ChildrenFacilitator.FACILITATOR_TYPE).build())
            .skipExpressionChain(false)
            .build();
    planNodes.add(sidecarsNode);
    return ImmutablePair.of(sidecarsNode.getUuid(), planNodes);
  }

  private PlanNode createPlanForArtifactNode(String identifier, ArtifactInfo artifactInfo) {
    return PlanNode.builder()
        .uuid(artifactInfo.getUuid())
        .stepType(ArtifactStep.STEP_TYPE)
        .name(PlanCreatorConstants.ARTIFACT_NODE_NAME)
        .identifier(identifier)
        .stepParameters(artifactInfo.getParams())
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(TaskFacilitator.FACILITATOR_TYPE).build())
        .skipExpressionChain(false)
        .build();
  }

  @Value
  private static class ArtifactList {
    ArtifactInfo primary;
    Map<String, ArtifactInfo> sidecars;
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  private static class ArtifactListBuilder {
    ArtifactInfoBuilder primary;
    Map<String, ArtifactInfoBuilder> sidecars;

    ArtifactListBuilder(ArtifactListConfig artifactListConfig) {
      if (artifactListConfig == null) {
        this.primary = null;
        this.sidecars = null;
        return;
      }

      PrimaryArtifact primarySpecWrapper = artifactListConfig.getPrimary();
      if (primarySpecWrapper != null) {
        this.primary = new ArtifactInfoBuilder(primarySpecWrapper.getUuid(),
            ArtifactStepParameters.builder()
                .type(primarySpecWrapper.getSourceType())
                .spec(primarySpecWrapper.getSpec()));
      } else {
        this.primary = null;
      }

      this.sidecars = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
        artifactListConfig.getSidecars().forEach(sc -> {
          SidecarArtifact sidecar = sc.getSidecar();
          this.sidecars.put(sidecar.getIdentifier(),
              new ArtifactInfoBuilder(sidecar.getUuid(),
                  ArtifactStepParameters.builder()
                      .identifier(sidecar.getIdentifier())
                      .type(sidecar.getSourceType())
                      .spec(sidecar.getSpec())));
        });
      }
    }

    ArtifactList build() {
      return new ArtifactList(primary == null ? null : primary.build(),
          sidecars == null
              ? null
              : sidecars.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }

    void addOverrideSets(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null
          || ParameterField.isNull(serviceConfig.getStageOverrides().getUseArtifactOverrideSets())) {
        return;
      }

      for (String useArtifactOverrideSet : serviceConfig.getStageOverrides().getUseArtifactOverrideSets().getValue()) {
        List<ArtifactOverrideSets> artifactOverrideSetsList =
            serviceConfig.getServiceDefinition()
                .getServiceSpec()
                .getArtifactOverrideSets()
                .stream()
                .map(ArtifactOverrideSetWrapper::getOverrideSet)
                .filter(overrideSet -> overrideSet.getIdentifier().equals(useArtifactOverrideSet))
                .collect(Collectors.toList());
        if (artifactOverrideSetsList.size() == 0) {
          throw new InvalidRequestException(
              String.format("Invalid identifier [%s] in artifact override sets", useArtifactOverrideSet));
        }
        if (artifactOverrideSetsList.size() > 1) {
          throw new InvalidRequestException(
              String.format("Duplicate identifier [%s] in artifact override sets", useArtifactOverrideSet));
        }

        ArtifactListConfig artifactListConfig = artifactOverrideSetsList.get(0).getArtifacts();
        addOverrides(artifactListConfig, ArtifactStepParametersBuilder::overrideSet);
      }
    }

    void addStageOverrides(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null || serviceConfig.getStageOverrides().getArtifacts() == null) {
        return;
      }
      ArtifactListConfig artifactListConfig = serviceConfig.getStageOverrides().getArtifacts();
      addOverrides(artifactListConfig, ArtifactStepParametersBuilder::stageOverride);
    }

    private void addOverrides(
        ArtifactListConfig artifactListConfig, BiConsumer<ArtifactStepParametersBuilder, ArtifactConfig> consumer) {
      PrimaryArtifact primarySpecWrapper = artifactListConfig.getPrimary();
      if (primarySpecWrapper != null) {
        if (primary == null) {
          primary = new ArtifactInfoBuilder(primarySpecWrapper.getUuid(), ArtifactStepParameters.builder());
        }
        consumer.accept(primary.getBuilder(), primarySpecWrapper.getSpec());
      }

      if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
        for (SidecarArtifactWrapper sidecarWrapper : artifactListConfig.getSidecars()) {
          SidecarArtifact sidecar = sidecarWrapper.getSidecar();
          ArtifactInfoBuilder artifactInfoBuilder = sidecars.computeIfAbsent(sidecar.getIdentifier(),
              identifier
              -> new ArtifactInfoBuilder(sidecar.getUuid(), ArtifactStepParameters.builder().identifier(identifier)));
          consumer.accept(artifactInfoBuilder.getBuilder(), sidecar.getSpec());
        }
      }
    }
  }

  @Value
  private static class ArtifactInfo {
    String uuid;
    ArtifactStepParameters params;
  }

  @Value
  private static class ArtifactInfoBuilder {
    String uuid;
    ArtifactStepParametersBuilder builder;

    ArtifactInfo build() {
      return new ArtifactInfo(uuid, builder.build());
    }
  }
}