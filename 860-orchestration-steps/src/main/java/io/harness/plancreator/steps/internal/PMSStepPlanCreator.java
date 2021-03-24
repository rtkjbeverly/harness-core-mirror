package io.harness.plancreator.steps.internal;

import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.steps.StepSpecTypeConstants;

import com.google.common.collect.Sets;
import java.util.Set;

public class PMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(
        StepSpecTypeConstants.BARRIER, "HarnessApproval", "JiraApproval", StepSpecTypeConstants.HTTP);
  }
}