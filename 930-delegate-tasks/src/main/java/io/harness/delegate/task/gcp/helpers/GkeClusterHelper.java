package io.harness.delegate.task.gcp.helpers;

import static io.harness.delegate.task.gcp.helpers.GcpHelperService.ALL_LOCATIONS;
import static io.harness.delegate.task.gcp.helpers.GcpHelperService.LOCATION_DELIMITER;
import static io.harness.eraro.ErrorCode.CLUSTER_NOT_FOUND;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesConfig.KubernetesConfigBuilder;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.api.services.container.model.MasterAuth;
import com.google.api.services.container.model.NodeConfig;
import com.google.api.services.container.model.Operation;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GkeClusterHelper {
  @Inject private GcpHelperService gcpHelperService = new GcpHelperService();
  @Inject private TimeLimiter timeLimiter;

  public KubernetesConfig createCluster(char[] serviceAccountKeyFileContent, boolean useDelegate,
      String locationClusterName, String namespace, Map<String, String> params) {
    Container gkeContainerService = gcpHelperService.getGkeContainerService(serviceAccountKeyFileContent, useDelegate);
    String projectId = getProjectIdFromCredentials(serviceAccountKeyFileContent);
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    // See if the cluster already exists
    try {
      Cluster cluster = gkeContainerService.projects()
                            .locations()
                            .clusters()
                            .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                            .execute();
      log.info("Cluster already exists");
      log.debug("Cluster {}, location {}, project {}", clusterName, location, projectId);
      return configFromCluster(cluster, namespace);
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "getting");
    }

    // Cluster doesn't exist. Create it.
    try {
      CreateClusterRequest content = new CreateClusterRequest().setCluster(
          new Cluster()
              .setName(clusterName)
              .setNodeConfig(new NodeConfig().setMachineType(params.get("machineType")))
              .setInitialNodeCount(Integer.valueOf(params.get("nodeCount")))
              .setMasterAuth(
                  new MasterAuth().setUsername(params.get("masterUser")).setPassword(params.get("masterPwd"))));
      Operation createOperation =
          gkeContainerService.projects()
              .locations()
              .clusters()
              .create("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName, content)
              .execute();
      String operationStatus =
          waitForOperationToComplete(createOperation, gkeContainerService, projectId, location, "Provisioning");
      if (operationStatus.equals("DONE")) {
        Cluster cluster = gkeContainerService.projects()
                              .locations()
                              .clusters()
                              .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                              .execute();
        log.info("Cluster status: {}", cluster.getStatus());
        log.debug("Master endpoint: {}", cluster.getEndpoint());
        return configFromCluster(cluster, namespace);
      }
    } catch (IOException e) {
      logNotFoundOrError(e, projectId, location, clusterName, "creating");
    }
    return null;
  }

  public KubernetesConfig getCluster(
      char[] serviceAccountKeyFileContent, boolean useDelegate, String locationClusterName, String namespace) {
    Container gkeContainerService = gcpHelperService.getGkeContainerService(serviceAccountKeyFileContent, useDelegate);
    String projectId = getProjectIdFromCredentials(serviceAccountKeyFileContent);
    String[] locationCluster = locationClusterName.split(LOCATION_DELIMITER);
    String location = locationCluster[0];
    String clusterName = locationCluster[1];
    try {
      Cluster cluster = gkeContainerService.projects()
                            .locations()
                            .clusters()
                            .get("projects/" + projectId + "/locations/" + location + "/clusters/" + clusterName)
                            .execute();
      log.debug("Found cluster {} in location {} for project {}", clusterName, location, projectId);
      log.info("Cluster status: {}", cluster.getStatus());
      log.debug("Master endpoint: {}", cluster.getEndpoint());
      return configFromCluster(cluster, namespace);
    } catch (IOException e) {
      // PL-1118: In case the cluster is being destroyed/torn down. Return null will immediately reclaim the service
      // instances
      if (e instanceof GoogleJsonResponseException
          && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        String errorMessage =
            format("Cluster %s does not exist in location %s for project %s", clusterName, location, projectId);
        log.warn(errorMessage, e);
        throw new WingsException(CLUSTER_NOT_FOUND, e).addParam("message", errorMessage);
      } else {
        String errorMessage =
            format("Error getting cluster %s in location %s for project %s", clusterName, location, projectId);
        log.error(errorMessage, e);
        throw new InvalidRequestException(errorMessage, e);
      }
    }
  }

  public List<String> listClusters(char[] serviceAccountKeyFileContent, boolean useDelegate) {
    Container gkeContainerService = gcpHelperService.getGkeContainerService(serviceAccountKeyFileContent, useDelegate);
    String projectId = getProjectIdFromCredentials(serviceAccountKeyFileContent);
    try {
      ListClustersResponse response = gkeContainerService.projects()
                                          .locations()
                                          .clusters()
                                          .list("projects/" + projectId + "/locations/" + ALL_LOCATIONS)
                                          .execute();
      List<Cluster> clusters = response.getClusters();
      return clusters != null ? clusters.stream()
                                    .map(cluster -> cluster.getZone() + LOCATION_DELIMITER + cluster.getName())
                                    .collect(toList())
                              : ImmutableList.of();
    } catch (IOException e) {
      log.error("Error listing clusters for project " + projectId, e);
    }
    return null;
  }

  private KubernetesConfig configFromCluster(Cluster cluster, String namespace) {
    MasterAuth masterAuth = cluster.getMasterAuth();
    KubernetesConfigBuilder kubernetesConfigBuilder = KubernetesConfig.builder()
                                                          .masterUrl("https://" + cluster.getEndpoint() + "/")
                                                          .namespace(isNotBlank(namespace) ? namespace : "default");
    if (masterAuth.getUsername() != null) {
      kubernetesConfigBuilder.username(masterAuth.getUsername().toCharArray());
    }
    if (masterAuth.getPassword() != null) {
      kubernetesConfigBuilder.password(masterAuth.getPassword().toCharArray());
    }
    if (masterAuth.getClusterCaCertificate() != null) {
      kubernetesConfigBuilder.caCert(masterAuth.getClusterCaCertificate().toCharArray());
    }
    if (masterAuth.getClientCertificate() != null) {
      kubernetesConfigBuilder.clientCert(masterAuth.getClientCertificate().toCharArray());
    }
    if (masterAuth.getClientKey() != null) {
      kubernetesConfigBuilder.clientKey(masterAuth.getClientKey().toCharArray());
    }
    return kubernetesConfigBuilder.build();
  }

  private String waitForOperationToComplete(Operation operation, Container gkeContainerService, String projectId,
      String location, String operationLogMessage) {
    log.info(operationLogMessage + "...");
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          String status =
              gkeContainerService.projects()
                  .locations()
                  .operations()
                  .get("projects/" + projectId + "/locations/" + location + "/operations/" + operation.getName())
                  .execute()
                  .getStatus();
          if (!status.equals("RUNNING")) {
            log.info(operationLogMessage + ": " + status);
            return status;
          }
          sleep(ofSeconds(gcpHelperService.getSleepIntervalSecs()));
        }
      }, gcpHelperService.getTimeoutMins(), TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      log.error("Timed out checking operation status");
      return "UNKNOWN";
    } catch (Exception e) {
      log.error("Error checking operation status", e);
      return "UNKNOWN";
    }
  }

  private String getProjectIdFromCredentials(char[] credentials) {
    return (String) ((DBObject) JSON.parse(new String(credentials))).get("project_id");
  }

  private void logNotFoundOrError(
      IOException e, String projectId, String location, String clusterName, String actionVerb) {
    if (e instanceof GoogleJsonResponseException
        && ((GoogleJsonResponseException) e).getDetails().getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
      log.warn(format("Cluster %s does not exist in location %s for project %s", clusterName, location, projectId), e);
    } else {
      log.error(
          format("Error %s cluster %s in location %s for project %s", actionVerb, clusterName, location, projectId), e);
    }
  }
}