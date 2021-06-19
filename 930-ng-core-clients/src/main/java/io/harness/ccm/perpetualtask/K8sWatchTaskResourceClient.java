package io.harness.ccm.perpetualtask;

import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.ng.core.dto.ResponseDTO;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface K8sWatchTaskResourceClient {
  String MANAGER_URL_PREFIX = "/api";
  String K8S_WATCH_TASK_RESOURCE_ENDPOINT = "/ccm/perpetual-task";

  String TASK_ID = "taskId";
  String ACCOUNT_ID = "accountId";

  @POST(MANAGER_URL_PREFIX + K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/create")
  Call<ResponseDTO<String>> create(
      @NotEmpty @Query(ACCOUNT_ID) String accountId, @NotNull @Body K8sEventCollectionBundle k8sEventCollectionBundle);

  @POST(MANAGER_URL_PREFIX + K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/reset")
  Call<ResponseDTO<Boolean>> reset(@NotEmpty @Query(ACCOUNT_ID) String accountId,
      @NotEmpty @Query(TASK_ID) String taskId, @NotNull @Body K8sEventCollectionBundle k8sEventCollectionBundle);

  @GET(MANAGER_URL_PREFIX + K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/delete")
  Call<ResponseDTO<Boolean>> delete(
      @NotEmpty @Query(ACCOUNT_ID) String accountId, @NotEmpty @Query(TASK_ID) String taskId);
}