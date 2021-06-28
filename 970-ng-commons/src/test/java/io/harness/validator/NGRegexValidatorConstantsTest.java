package io.harness.validator;

import static io.harness.rule.OwnerRule.SAMARTH;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGRegexValidatorConstantsTest {
  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testTimeoutPattern() {
    Pattern pattern = Pattern.compile(NGRegexValidatorConstants.TIMEOUT_PATTERN);

    // Valid cases
    assertTrue(pattern.matcher("1m").matches());
    assertTrue(pattern.matcher("1m20s").matches());
    assertTrue(pattern.matcher("1m 20s").matches());

    // Runtime
    assertTrue(pattern.matcher("<+input>").matches());
    assertTrue(pattern.matcher("<+input>.allowedValues()").matches());
    assertTrue(pattern.matcher("<+input>.regex()").matches());

    // Invalid cases
    assertFalse(pattern.matcher("1m  20s").matches());
    assertFalse(pattern.matcher("1m 8").matches());
    assertFalse(pattern.matcher("18").matches());
    assertFalse(pattern.matcher("m").matches());
    assertFalse(pattern.matcher("20mm").matches());
    assertFalse(pattern.matcher("m20m").matches());
    assertFalse(pattern.matcher(" 1m").matches());
    assertFalse(pattern.matcher("1m ").matches());
    assertFalse(pattern.matcher("1 m").matches());
    assertFalse(pattern.matcher("1a").matches());
    assertFalse(pattern.matcher("<+random>").matches());
    assertFalse(pattern.matcher("random").matches());
  }
}