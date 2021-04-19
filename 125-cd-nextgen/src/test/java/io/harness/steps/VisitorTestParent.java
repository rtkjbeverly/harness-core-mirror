package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@SimpleVisitorHelper(helperClass = VisitorTestParentVisitorHelper.class)
public class VisitorTestParent implements Visitable {
  String name;
  VisitorTestChild visitorTestChild;

  @Override
  public LevelNode getLevelNode() {
    return null;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add("visitorTestChild", visitorTestChild);
    return visitableChildren;
  }
}