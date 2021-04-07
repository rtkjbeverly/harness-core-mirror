package io.harness.invites.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface NgInviteClient {
  String INVITE_POST_SIGNUP_API = "invites/complete/";
  String INVITE_ACCEPT = "invites/accept/";

  @GET(INVITE_POST_SIGNUP_API) Call<ResponseDTO<Boolean>> completeInvite(@Query("token") String token);

  @GET(INVITE_ACCEPT) Call<ResponseDTO<InviteAcceptResponse>> accept(@Query("token") String token);
}