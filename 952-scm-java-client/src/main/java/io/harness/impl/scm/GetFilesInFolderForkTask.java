package io.harness.impl.scm;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import static java.util.stream.Collectors.toList;

import io.harness.product.ci.scm.proto.ContentType;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import lombok.Builder;

/**
 * This is a fork and join task which we are using to get all files belonging in
 * the folders.
 */
public class GetFilesInFolderForkTask extends RecursiveTask<List<FileChange>> {
  SCMGrpc.SCMBlockingStub scmBlockingStub;
  private String folderPath;
  private Provider provider;
  private String branch;
  private String slug;

  @Builder
  public GetFilesInFolderForkTask(
      String folderPath, Provider provider, String branch, String slug, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    this.folderPath = folderPath;
    this.provider = provider;
    this.branch = branch;
    this.slug = slug;
    this.scmBlockingStub = scmBlockingStub;
  }

  /**
   * This is the main function which will be called by all the tasks, every task needs to
   * get files belonging to its folder.
   *
   * This function provides all the files belonging to this folder and then it creates
   * subtasks for every sub folder
   */
  @Override
  protected List<FileChange> compute() {
    List<FileChange> filesList = new ArrayList<>();
    FindFilesInBranchRequest findFilesInBranchRequest = FindFilesInBranchRequest.newBuilder()
                                                            .setBranch(branch)
                                                            .setSlug(slug)
                                                            .setProvider(provider)
                                                            .setPath(folderPath)
                                                            .build();
    List<FileChange> filesInBranch = getAllFilesPresentInFolder(findFilesInBranchRequest);
    List<String> newFoldersToBeProcessed = getListOfNewFoldersToBeProcessed(filesInBranch);
    List<GetFilesInFolderForkTask> tasksForSubFolders = createTasksForSubFolders(newFoldersToBeProcessed);
    addFilesOfThisFolder(filesList, filesInBranch);
    addFilesOfSubfolders(filesList, tasksForSubFolders);
    return filesList;
  }

  List<FileChange> getAllFilesPresentInFolder(FindFilesInBranchRequest findFilesInBranchRequest) {
    FindFilesInBranchResponse filesInBranchResponse = null;
    List<FileChange> allFilesInThisFolder = new ArrayList<>();
    do {
      filesInBranchResponse = scmBlockingStub.findFilesInBranch(findFilesInBranchRequest);
      allFilesInThisFolder.addAll(filesInBranchResponse.getFileList());
    } while (hasMoreFiles(filesInBranchResponse));
    return allFilesInThisFolder;
  }

  private boolean hasMoreFiles(FindFilesInBranchResponse filesInBranchResponse) {
    return filesInBranchResponse != null && filesInBranchResponse.getPagination() != null
        && filesInBranchResponse.getPagination().getNext() == 1;
  }

  private List<GetFilesInFolderForkTask> createTasksForSubFolders(List<String> newFoldersToBeProcessed) {
    List<GetFilesInFolderForkTask> tasks = new ArrayList<>();
    for (String folder : newFoldersToBeProcessed) {
      GetFilesInFolderForkTask task = GetFilesInFolderForkTask.builder()
                                          .branch(branch)
                                          .folderPath(folder)
                                          .provider(provider)
                                          .scmBlockingStub(scmBlockingStub)
                                          .slug(slug)
                                          .build();
      task.fork();
      tasks.add(task);
    }
    return tasks;
  }

  private void addFilesOfSubfolders(List<FileChange> filesList, List<GetFilesInFolderForkTask> tasksForSubFolders) {
    for (GetFilesInFolderForkTask task : tasksForSubFolders) {
      filesList.addAll(task.join());
    }
  }

  private void addFilesOfThisFolder(List<FileChange> filesList, List<FileChange> filesInBranchResponse) {
    List<FileChange> fileChangesInThisFolder = emptyIfNull(filesInBranchResponse)
                                                   .stream()
                                                   .filter(change -> change.getContentType() == ContentType.FILE)
                                                   .collect(toList());
    filesList.addAll(fileChangesInThisFolder);
  }

  private List<String> getListOfNewFoldersToBeProcessed(List<FileChange> filesInBranch) {
    return emptyIfNull(filesInBranch)
        .stream()
        .filter(fileChange -> fileChange.getContentType() == ContentType.DIRECTORY)
        .map(FileChange::getPath)
        .collect(toList());
  }

  public List<FileChange> createForkJoinTask(List<String> foldersList) {
    List<GetFilesInFolderForkTask> tasks = new ArrayList<>();
    ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    for (String folder : foldersList) {
      GetFilesInFolderForkTask task = GetFilesInFolderForkTask.builder()
                                          .branch(branch)
                                          .folderPath(folder)
                                          .provider(provider)
                                          .scmBlockingStub(scmBlockingStub)
                                          .slug(slug)
                                          .build();
      forkJoinPool.execute(task);
      tasks.add(task);
    }
    List<FileChange> allFiles = new ArrayList<>();
    for (GetFilesInFolderForkTask task : tasks) {
      allFiles.addAll(emptyIfNull(task.join()));
    }
    return allFiles;
  }
}