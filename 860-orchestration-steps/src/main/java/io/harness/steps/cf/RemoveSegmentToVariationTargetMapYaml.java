package io.harness.steps.cf;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("RemoveSegmentToVariationTargetMap")
@TypeAlias("RemoveSegmentToVariationTargetMapYaml")
public class RemoveSegmentToVariationTargetMapYaml implements PatchInstruction {
  @Builder.Default @NotNull private PatchInstruction.Type type = Type.REMOVE_SEGMENT_TO_VARIATION_TARGET_MAP;
  @NotNull private RemoveSegmentToVariationTargetMapYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RemoveSegmentToVariationTargetMapYamlSpec {
    private String variation;
    private List<String> segments;
  }
}