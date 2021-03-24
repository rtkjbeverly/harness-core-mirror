package io.harness.gitsync.gittoharness;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.FileProcessingResponse;
import io.harness.gitsync.FileProcessingStatus;
import io.harness.gitsync.ProcessingFailureStage;
import io.harness.gitsync.ProcessingResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GitToHarnessProcessorImpl implements GitToHarnessProcessor {
  ChangeSetInterceptorService changeSetInterceptorService;
  ChangeSetHelperService changeSetHelperService;
  Supplier<List<EntityType>> sortOrder;

  @Inject
  public GitToHarnessProcessorImpl(ChangeSetInterceptorService changeSetInterceptorService,
      ChangeSetHelperService changeSetHelperService, @Named("GitSyncSortOrder") Supplier<List<EntityType>> sortOrder) {
    this.changeSetInterceptorService = changeSetInterceptorService;
    this.changeSetHelperService = changeSetHelperService;
    this.sortOrder = sortOrder;
  }

  /**
   * Processing is a 4 step process.
   * <li><b>Preprocess step.</b> Generally a no op stage.</li>
   * <li><b>Sort step.</b> Changesets are sorted as per sort order.</li>
   * <li><b>Process step.</b> Change sets are processed by calling various service layers.</li>
   * <li><b>Post process step.</b> Collection of all the return data happens.</li>
   */
  @Override
  public ProcessingResponse process(ChangeSets changeSets) {
    Map<String, FileProcessingResponse> processingResponseMap = initializeProcessingResponse(changeSets);
    String accountId = changeSets.getAccountId();

    if (isEmpty(changeSets.getChangeSetList())) {
      return ProcessingResponse.newBuilder().setAccountId(accountId).build();
    }
    if (preProcessStage(changeSets, processingResponseMap)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.RECEIVE_STAGE);
    }

    if (sortStage(changeSets, processingResponseMap)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.SORT_STAGE);
    }
    if (processStage(changeSets, processingResponseMap)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.PROCESS_STAGE);
    }

    if (postProcessStage(changeSets, processingResponseMap, accountId)) {
      return flattenProcessingResponse(processingResponseMap, accountId, ProcessingFailureStage.PROCESS_STAGE);
    }

    return flattenProcessingResponse(processingResponseMap, accountId, null);
  }

  private boolean postProcessStage(
      ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap, String accountId) {
    try {
      final ProcessingResponse processingResponse = flattenProcessingResponse(processingResponseMap, accountId, null);
      changeSetInterceptorService.postChangeSetProcessing(processingResponse, changeSets.getAccountId());
    } catch (Exception e) {
      return true;
    }
    return false;
  }

  private boolean processStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap) {
    try {
      // todo(abhinav): Do parallel processing.
      for (ChangeSet changeSet : changeSets.getChangeSetList()) {
        try {
          changeSetHelperService.process(changeSet);
          updateFileProcessingResponse(FileProcessingStatus.SUCCESS, null, processingResponseMap, changeSet.getId());
          processingResponseMap.put(changeSet.getId(),
              FileProcessingResponse.newBuilder()
                  .setAccountId(changeSet.getAccountId())
                  .setId(changeSet.getId())
                  .setStatus(FileProcessingStatus.SUCCESS)
                  .build());
        } catch (Exception e) {
          updateFileProcessingResponse(
              FileProcessingStatus.FAILURE, e.getMessage(), processingResponseMap, changeSet.getId());
        }
      }
    } catch (Exception e) {
      return true;
    }
    return false;
  }

  private boolean sortStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap) {
    try {
      final List<ChangeSet> sortedChangeSets = sortChangeSets(sortOrder, changeSets.getChangeSetList());
      changeSetInterceptorService.postChangeSetSort(sortedChangeSets, changeSets.getAccountId());
    } catch (Exception e) {
      updateFileProcessingResponseForAllChangeSets(processingResponseMap, FileProcessingStatus.SKIPPED, e.getMessage());
      return true;
    }
    return false;
  }

  private boolean preProcessStage(ChangeSets changeSets, Map<String, FileProcessingResponse> processingResponseMap) {
    try {
      changeSetInterceptorService.onChangeSetReceive(changeSets, changeSets.getAccountId());
    } catch (Exception e) {
      updateFileProcessingResponseForAllChangeSets(processingResponseMap, FileProcessingStatus.SKIPPED, e.getMessage());
      return true;
    }
    return false;
  }

  private ProcessingResponse flattenProcessingResponse(Map<String, FileProcessingResponse> processingResponseMap,
      String accountId, ProcessingFailureStage processingFailureStage) {
    final List<FileProcessingResponse> fileProcessingResponses = new ArrayList<>(processingResponseMap.values());
    final ProcessingResponse.Builder processingResponseBuilder =
        ProcessingResponse.newBuilder().addAllResponse(fileProcessingResponses).setAccountId(accountId);
    if (processingFailureStage != null) {
      processingResponseBuilder.setProcessingFailureStage(processingFailureStage);
    }
    return processingResponseBuilder.build();
  }

  private void updateFileProcessingResponseForAllChangeSets(
      Map<String, FileProcessingResponse> processingResponseMap, FileProcessingStatus status, String message) {
    processingResponseMap.forEach(
        (key, value) -> updateFileProcessingResponse(status, message, processingResponseMap, key));
  }

  private void updateFileProcessingResponse(
      FileProcessingStatus status, String message, Map<String, FileProcessingResponse> response, String key) {
    final FileProcessingResponse responseValue = response.get(key);
    final FileProcessingResponse.Builder fileProcessingResponseBuilder = FileProcessingResponse.newBuilder()
                                                                             .setStatus(status)
                                                                             .setAccountId(responseValue.getAccountId())
                                                                             .setId(responseValue.getId());
    if (!isEmpty(message)) {
      fileProcessingResponseBuilder.setErrorMsg(message);
    }
    response.put(key, fileProcessingResponseBuilder.build());
  }

  private Map<String, FileProcessingResponse> initializeProcessingResponse(ChangeSets changeSets) {
    Map<String, FileProcessingResponse> processingResponseMap = new HashMap<>();
    changeSets.getChangeSetList().forEach(changeSet
        -> processingResponseMap.put(changeSet.getId(),
            FileProcessingResponse.newBuilder()
                .setId(changeSet.getId())
                .setAccountId(changeSet.getAccountId())
                .setStatus(FileProcessingStatus.UNPROCESSED)
                .build()));
    return processingResponseMap;
  }

  private List<ChangeSet> sortChangeSets(Supplier<List<EntityType>> sortOrder, List<ChangeSet> changeSetList) {
    return changeSetList.stream().sorted(new ChangeSetSortComparator(sortOrder.get())).collect(Collectors.toList());
  }
}