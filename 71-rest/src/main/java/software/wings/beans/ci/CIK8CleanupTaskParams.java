package software.wings.beans.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ci.pod.ConnectorDetails;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8CleanupTaskParams implements CICleanupTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private List<String> podNameList;
  @NotNull private List<String> serviceNameList;
  @NotNull private String namespace;
  @Builder.Default private static final CICleanupTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}