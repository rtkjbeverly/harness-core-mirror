package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.k8s.model.Release.Status.Failed;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import io.harness.exception.KubernetesYamlException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.DescribeCommand;
import io.harness.k8s.kubectl.GetCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.RolloutStatusCommand;
import io.harness.k8s.kubectl.ScaleCommand;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  private static String eventOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,MESSAGE:.message,REASON:.reason";

  public static boolean applyManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8SDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8SDelegateTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true);

    executionLogCallback.saveExecutionLog(applyCommand.command() + "\n");

    ProcessResult result = applyCommand.execute(k8SDelegateTaskParams.getWorkingDirectory(),
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, INFO);
          }
        },
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, ERROR);
          }
        });

    if (result.getExitValue() != 0) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public static boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      K8sDelegateTaskParams k8SDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    final String eventFormat = "%-7s: %s";
    final String statusFormat = "%n%-7s: %s";

    GetCommand getEventsCommand =
        client.get().resources("events").namespace(resourceId.getNamespace()).output(eventOutputFormat).watchOnly(true);

    executionLogCallback.saveExecutionLog(getEventsCommand.command() + "\n");

    boolean success = false;

    StartedProcess eventWatchProcess = null;
    try {
      eventWatchProcess = getEventsCommand.executeInBackground(k8SDelegateTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              if (line.contains(resourceId.getName())) {
                executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), INFO);
              }
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), ERROR);
            }
          });

      RolloutStatusCommand rolloutStatusCommand =
          client.rollout().status().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace()).watch(true);

      executionLogCallback.saveExecutionLog(rolloutStatusCommand.command() + "\n");

      ProcessResult result = rolloutStatusCommand.execute(k8SDelegateTaskParams.getWorkingDirectory(),
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), INFO);
            }
          },
          new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), ERROR);
            }
          });

      success = result.getExitValue() == 0;

      if (!success) {
        logger.warn(result.outputString());
      }
      return success;
    } finally {
      if (eventWatchProcess != null) {
        eventWatchProcess.getProcess().destroyForcibly().waitFor();
      }
      if (success) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      }
    }
  }

  public static boolean scale(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams,
      KubernetesResourceId resourceId, int targetReplicaCount, ExecutionLogCallback executionLogCallback)
      throws Exception {
    executionLogCallback.saveExecutionLog("\nScaling " + resourceId.kindNameRef());

    ScaleCommand scaleCommand = client.scale().resource(resourceId.kindNameRef()).replicas(targetReplicaCount);

    executionLogCallback.saveExecutionLog("\n" + scaleCommand.command());

    try (LogOutputStream logOutputStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(line, INFO);
               }
             };

         LogOutputStream logErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(line, ERROR);
               }
             }) {
      ProcessResult result =
          scaleCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        return true;
      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        logger.warn("Failed to scale resource. Error {}", result.getOutput());
        return false;
      }
    }
  }

  public static void cleanup(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams, ReleaseHistory releaseHistory,
      ExecutionLogCallback executionLogCallback) throws Exception {
    final int lastSuccessfulReleaseNumber =
        (releaseHistory.getLastSuccessfulRelease() != null) ? releaseHistory.getLastSuccessfulRelease().getNumber() : 0;

    if (lastSuccessfulReleaseNumber == 0) {
      executionLogCallback.saveExecutionLog("\nNo previous successful release found.");
    } else {
      executionLogCallback.saveExecutionLog("\nPrevious Successful Release is " + lastSuccessfulReleaseNumber);
    }

    executionLogCallback.saveExecutionLog("\nCleaning up older and failed releases");

    for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
      Release release = releaseHistory.getReleases().get(releaseIndex);
      if (release.getNumber() < lastSuccessfulReleaseNumber || release.getStatus() == Failed) {
        for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
          KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
          if (resourceId.isVersioned()) {
            DeleteCommand deleteCommand =
                client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace());

            executionLogCallback.saveExecutionLog("\n" + deleteCommand.command());

            ProcessResult result = deleteCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(),
                new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line, INFO);
                  }
                },
                new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line, ERROR);
                  }
                });

            if (result.getExitValue() != 0) {
              logger.warn("Failed to delete resource {}. Error {}", resourceId.kindNameRef(), result.getOutput());
            }
          }
        }
      }
    }
    releaseHistory.getReleases().removeIf(
        release -> release.getNumber() < lastSuccessfulReleaseNumber || release.getStatus() == Failed);
  }

  public static void describe(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws Exception {
    DescribeCommand describeCommand = client.describe().filename("manifests.yaml");

    executionLogCallback.saveExecutionLog(describeCommand.command() + "\n");

    describeCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(),
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, INFO);
          }
        },
        new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line, ERROR);
          }
        });
  }

  public static String getLatestRevision(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8SDelegateTaskParams) throws Exception {
    RolloutHistoryCommand rolloutHistoryCommand =
        client.rollout().history().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace());

    try (LogOutputStream logOutputStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             };
         LogOutputStream logErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             }) {
      ProcessResult result =
          rolloutHistoryCommand.execute(k8SDelegateTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        return parseLatestRevisionNumberFromRolloutHistory(result.outputString());
      }
    }
    return "";
  }

  public static Integer getCurrentReplicas(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8SDelegateTaskParams) throws Exception {
    GetCommand getCommand = client.get()
                                .resources(resourceId.kindNameRef())
                                .namespace(resourceId.getNamespace())
                                .output("jsonpath={$.spec.replicas}");

    try (LogOutputStream logOutputStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             };
         LogOutputStream logErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {}
             }) {
      ProcessResult result =
          getCommand.execute(k8SDelegateTaskParams.getWorkingDirectory(), logOutputStream, logErrorStream);

      if (result.getExitValue() == 0) {
        return Integer.valueOf(result.outputString());
      } else {
        return null;
      }
    }
  }

  public static List<ManifestFile> renderTemplate(K8sDelegateTaskParams k8SDelegateTaskParams,
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) throws Exception {
    Optional<ManifestFile> valuesFile =
        manifestFiles.stream()
            .filter(manifestFile -> StringUtils.equals(values_filename, manifestFile.getFileName()))
            .findFirst();

    if (!valuesFile.isPresent()) {
      executionLogCallback.saveExecutionLog("No values.yaml file found. Skipping template rendering.");
      return manifestFiles;
    }

    FileIo.writeUtf8StringToFile(
        k8SDelegateTaskParams.getWorkingDirectory() + '/' + values_filename, valuesFile.get().getFileContent());

    List<ManifestFile> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
        continue;
      }

      FileIo.writeUtf8StringToFile(
          k8SDelegateTaskParams.getWorkingDirectory() + "/template.yaml", manifestFile.getFileContent());

      ProcessExecutor processExecutor =
          new ProcessExecutor()
              .timeout(10, TimeUnit.SECONDS)
              .directory(new File(k8SDelegateTaskParams.getWorkingDirectory()))
              .commandSplit(encloseWithQuotesIfNeeded(k8SDelegateTaskParams.getGoTemplateClientPath())
                  + " -t template.yaml -f " + values_filename)
              .readOutput(true);
      ProcessResult processResult = processExecutor.execute();
      result.add(ManifestFile.builder()
                     .fileName(manifestFile.getFileName())
                     .fileContent(processResult.outputString())
                     .build());
    }

    return result;
  }

  public static List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (!StringUtils.equals(values_filename, manifestFile.getFileName())) {
        try {
          result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          if (e instanceof KubernetesYamlException) {
            executionLogCallback.saveExecutionLog(e.getMessage(), ERROR);
          } else {
            executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
          }
          throw e;
        }
      }
    }

    return result.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());
  }

  public static String getResourcesInTableFormat(List<KubernetesResource> resources) {
    StringBuilder sb = new StringBuilder(1024);
    final String tableFormat = "%-20s%-40s%-6s";
    sb.append(System.lineSeparator())
        .append(format(tableFormat, "Kind", "Name", "Versioned"))
        .append(System.lineSeparator())
        .append(format(tableFormat, "----", "----", "---------"))
        .append(System.lineSeparator());

    for (KubernetesResource resource : resources) {
      KubernetesResourceId id = resource.getResourceId();
      sb.append(format(tableFormat, id.getKind(), id.getName(), id.isVersioned())).append(System.lineSeparator());
    }

    return sb.toString();
  }

  private static List<ManifestFile> getManifesFilesFromGit(
      K8sDelegateManifestConfig delegateManifestConfig, GitService gitService, EncryptionService encryptionService) {
    GitFileConfig gitFileConfig = delegateManifestConfig.getGitFileConfig();
    GitConfig gitConfig = delegateManifestConfig.getGitConfig();

    encryptionService.decrypt(gitConfig, delegateManifestConfig.getEncryptedDataDetails());

    GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(delegateManifestConfig.getGitConfig(),
        gitFileConfig.getConnectorId(), gitFileConfig.getCommitId(), gitFileConfig.getBranch(),
        asList(gitFileConfig.getFilePath()), gitFileConfig.isUseBranch());

    return manifestFilesFromGitFetchFilesResult(gitFetchFilesResult, gitFileConfig.getFilePath());
  }

  public static List<ManifestFile> fetchManifestFiles(K8sDelegateManifestConfig delegateManifestConfig,
      ExecutionLogCallback executionLogCallback, GitService gitService, EncryptionService encryptionService) {
    if (StoreType.Local.equals(delegateManifestConfig.getManifestStoreTypes())) {
      return delegateManifestConfig.getManifestFiles();
    }

    if (isBlank(delegateManifestConfig.getGitFileConfig().getFilePath())) {
      delegateManifestConfig.getGitFileConfig().setFilePath(StringUtils.EMPTY);
    }
    String filePath = delegateManifestConfig.getGitFileConfig().getFilePath();
    executionLogCallback.saveExecutionLog("\nFetching manifest files at path: " + (isBlank(filePath) ? "." : filePath));

    try {
      List<ManifestFile> manifestFilesFromGit =
          getManifesFilesFromGit(delegateManifestConfig, gitService, encryptionService);
      executionLogCallback.saveExecutionLog("Successfully fetched manifest files");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return manifestFilesFromGit;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return null;
    }
  }

  private static String getRelativePath(GitFile gitFile, String prefixPath) {
    if (isBlank(prefixPath)) {
      return gitFile.getFilePath();
    }

    return gitFile.getFilePath().substring(prefixPath.length());
  }

  public static String getValuesYamlGitFilePath(String filePath) {
    if (isBlank(filePath)) {
      return values_filename;
    }

    return normalizeFilePath(filePath) + values_filename;
  }

  public static String normalizeFilePath(String filePath) {
    if (isBlank(filePath)) {
      return filePath;
    }

    return filePath.endsWith("/") ? filePath : filePath + "/";
  }

  public static List<ManifestFile> manifestFilesFromGitFetchFilesResult(
      GitFetchFilesResult gitFetchFilesResult, String prefixPath) {
    List<ManifestFile> manifestFiles = new ArrayList<>();

    if (isNotEmpty(gitFetchFilesResult.getFiles())) {
      List<GitFile> files = gitFetchFilesResult.getFiles();

      for (GitFile gitFile : files) {
        String filePath = getRelativePath(gitFile, prefixPath);
        manifestFiles.add(ManifestFile.builder().fileName(filePath).fileContent(gitFile.getFileContent()).build());
      }
    }

    return manifestFiles;
  }
}
