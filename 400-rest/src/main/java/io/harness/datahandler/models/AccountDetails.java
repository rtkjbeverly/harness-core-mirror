package io.harness.datahandler.models;

import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.license.CeLicenseInfo;

import software.wings.beans.LicenseInfo;

import lombok.Data;

@OwnedBy(GTM)
@Data
@TargetModule(_955_ACCOUNT_MGMT)
public class AccountDetails {
  private String accountId;
  private String accountName;
  private String companyName;
  private String cluster;
  private LicenseInfo licenseInfo;
  private CeLicenseInfo ceLicenseInfo;
}