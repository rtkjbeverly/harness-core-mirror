package io.harness.ng.core.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.PROJECT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.dto.ProjectDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Getter
@NoArgsConstructor
public class ProjectUpdateEvent implements Event {
  private ProjectDTO newProject;
  private ProjectDTO oldProject;

  private String accountIdentifier;

  public ProjectUpdateEvent(String accountIdentifier, ProjectDTO newProject, ProjectDTO oldProject) {
    this.newProject = newProject;
    this.oldProject = oldProject;
    this.accountIdentifier = accountIdentifier;
  }

  @Override
  public ResourceScope getResourceScope() {
    return new ProjectScope(accountIdentifier, newProject.getOrgIdentifier(), newProject.getIdentifier());
  }

  @Override
  public Resource getResource() {
    return Resource.builder().identifier(oldProject.getIdentifier()).type(PROJECT).build();
  }

  @Override
  public String getEventType() {
    return "ProjectUpdated";
  }
}