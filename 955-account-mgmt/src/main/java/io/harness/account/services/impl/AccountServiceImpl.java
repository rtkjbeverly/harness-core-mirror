package io.harness.account.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.account.AccountClient;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.signup.dto.SignupDTO;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class AccountServiceImpl implements AccountService {
  private final AccountClient accountClient;

  public AccountDTO createAccount(SignupDTO dto) throws WingsException {
    String username = dto.getEmail().split("@")[0];

    AccountDTO accountDTO = AccountDTO.builder().name(username).companyName(username).build();

    return RestClientUtils.getResponse(accountClient.create(accountDTO));
  }
}