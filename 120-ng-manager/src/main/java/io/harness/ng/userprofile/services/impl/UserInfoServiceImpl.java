
package io.harness.ng.userprofile.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(HarnessTeam.PL)
public class UserInfoServiceImpl implements UserInfoService {
  @Inject private UserClient userClient;

  @Override
  public UserInfo get() {
    Optional<String> userEmail = getUserEmail();
    if (userEmail.isPresent()) {
      Optional<UserInfo> userInfo = RestClientUtils.getResponse(userClient.getUserFromEmail(userEmail.get()));
      return userInfo.get();
    } else {
      throw new IllegalStateException("user login required");
    }
  }

  @Override
  public UserInfo update(UserInfo userInfo) {
    Optional<String> userEmail = getUserEmail();
    if (userEmail.isPresent()) {
      userInfo.setEmail(userEmail.get());
      Optional<UserInfo> updatedUserInfo = RestClientUtils.getResponse(userClient.updateUser(userInfo));
      return updatedUserInfo.get();
    } else {
      throw new IllegalStateException("user login required");
    }
  }

  private Optional<String> getUserEmail() {
    String userEmail = null;
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      userEmail = ((UserPrincipal) (SourcePrincipalContextBuilder.getSourcePrincipal())).getEmail();
    }
    return Optional.of(userEmail);
  }
}