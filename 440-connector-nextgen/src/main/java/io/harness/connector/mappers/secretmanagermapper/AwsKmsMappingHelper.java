package io.harness.connector.mappers.secretmanagermapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType.ASSUME_IAM_ROLE;
import static io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType.ASSUME_STS_ROLE;
import static io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType.MANUAL_CONFIG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsConnector.AwsKmsConnectorBuilder;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsCredentialSpec;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsIamCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsManualCredential;
import io.harness.connector.entities.embedded.awskmsconnector.AwsKmsStsCredential;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorCredentialDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO.AwsKmsConnectorDTOBuilder;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeIAMDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecAssumeSTSDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialSpecManualConfigDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType;
import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsCredentialSpecConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsIamCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsManualCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsStsCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.BaseAwsKmsConfigDTO;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class AwsKmsMappingHelper {
  public AwsKmsConnectorDTOBuilder buildFromManualConfig(AwsKmsManualCredential credentialSpec) {
    AwsKmsCredentialSpecManualConfigDTO configDTO = AwsKmsCredentialSpecManualConfigDTO.builder()
                                                        .secretKey(credentialSpec.getSecretKey())
                                                        .accessKey(credentialSpec.getAccessKey())
                                                        .build();

    return populateCredentialDTO(populateCredentialSpec(configDTO, MANUAL_CONFIG));
  }

  public AwsKmsConnectorDTOBuilder buildFromIAMConfig(AwsKmsIamCredential credentialSpec) {
    AwsKmsCredentialSpecAssumeIAMDTO configDTO =
        AwsKmsCredentialSpecAssumeIAMDTO.builder().delegateSelectors(credentialSpec.getDelegateSelectors()).build();

    return populateCredentialDTO(populateCredentialSpec(configDTO, ASSUME_IAM_ROLE));
  }

  public AwsKmsConnectorDTOBuilder buildFromSTSConfig(AwsKmsStsCredential credentialSpec) {
    AwsKmsCredentialSpecAssumeSTSDTO configDTO = AwsKmsCredentialSpecAssumeSTSDTO.builder()
                                                     .delegateSelectors(credentialSpec.getDelegateSelectors())
                                                     .externalName(credentialSpec.getExternalName())
                                                     .roleArn(credentialSpec.getRoleArn())
                                                     .assumeStsRoleDuration(credentialSpec.getAssumeStsRoleDuration())
                                                     .build();

    return populateCredentialDTO(populateCredentialSpec(configDTO, ASSUME_STS_ROLE));
  }

  public AwsKmsConnectorBuilder buildSTSConfig(AwsKmsCredentialSpecAssumeSTSDTO config) {
    int assumeStsRoleDuration = config.getAssumeStsRoleDuration();
    if (assumeStsRoleDuration == 0) {
      assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
    }

    AwsKmsStsCredential build = AwsKmsStsCredential.builder()
                                    .delegateSelectors(config.getDelegateSelectors())
                                    .externalName(config.getExternalName())
                                    .roleArn(config.getRoleArn())
                                    .assumeStsRoleDuration(assumeStsRoleDuration)
                                    .build();
    return getAwsKmsConnectorBuilder(build, ASSUME_STS_ROLE);
  }

  public AwsKmsConnectorBuilder buildIAMConfig(AwsKmsCredentialSpecAssumeIAMDTO config) {
    AwsKmsIamCredential build = AwsKmsIamCredential.builder().delegateSelectors(config.getDelegateSelectors()).build();
    return getAwsKmsConnectorBuilder(build, ASSUME_IAM_ROLE);
  }

  public AwsKmsConnectorBuilder buildManualConfig(AwsKmsCredentialSpecManualConfigDTO credentialConfig) {
    AwsKmsManualCredential build = AwsKmsManualCredential.builder()
                                       .secretKey(credentialConfig.getSecretKey())
                                       .accessKey(credentialConfig.getAccessKey())
                                       .build();
    return getAwsKmsConnectorBuilder(build, MANUAL_CONFIG);
  }

  public AwsKmsConnectorDTO configDTOToConnectorDTO(AwsKmsConfigDTO configDTO) {
    BaseAwsKmsConfigDTO baseAwsKmsConfigDTO = configDTO.getBaseAwsKmsConfigDTO();
    return AwsKmsConnectorDTO.builder()
        .kmsArn(baseAwsKmsConfigDTO.getKmsArn())
        .region(baseAwsKmsConfigDTO.getRegion())
        .credential(populateCredential(baseAwsKmsConfigDTO))
        .build();
  }

  private AwsKmsConnectorCredentialDTO populateCredentialSpec(
      AwsKmsCredentialSpecDTO configDTO, AwsKmsCredentialType manualConfig) {
    return AwsKmsConnectorCredentialDTO.builder().credentialType(manualConfig).config(configDTO).build();
  }

  private AwsKmsConnectorDTOBuilder populateCredentialDTO(AwsKmsConnectorCredentialDTO credentialDTO) {
    return AwsKmsConnectorDTO.builder().credential(credentialDTO);
  }

  private AwsKmsConnectorBuilder getAwsKmsConnectorBuilder(
      AwsKmsCredentialSpec build, AwsKmsCredentialType manualConfig) {
    return AwsKmsConnector.builder().credentialType(manualConfig).credentialSpec(build);
  }

  private AwsKmsConnectorCredentialDTO populateCredential(BaseAwsKmsConfigDTO baseAwsKmsConfigDTO) {
    return AwsKmsConnectorCredentialDTO.builder()
        .credentialType(baseAwsKmsConfigDTO.getCredentialType())
        .config(populateCredentials(baseAwsKmsConfigDTO))
        .build();
  }

  private AwsKmsCredentialSpecDTO populateCredentials(BaseAwsKmsConfigDTO baseAwsKmsConfigDTO) {
    AwsKmsCredentialSpecConfig credential = baseAwsKmsConfigDTO.getCredential();
    AwsKmsCredentialType credentialType = baseAwsKmsConfigDTO.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        return buildFromManualConfig((AwsKmsManualCredentialConfig) credential);
      case ASSUME_IAM_ROLE:
        return buildFromIamConfig((AwsKmsIamCredentialConfig) credential);
      case ASSUME_STS_ROLE:
        return buildFromStsConfig((AwsKmsStsCredentialConfig) credential);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private AwsKmsCredentialSpecManualConfigDTO buildFromManualConfig(AwsKmsManualCredentialConfig credential) {
    return AwsKmsCredentialSpecManualConfigDTO.builder()
        .secretKey(credential.getSecretKey())
        .accessKey(credential.getAccessKey())
        .build();
  }

  private AwsKmsCredentialSpecAssumeIAMDTO buildFromIamConfig(AwsKmsIamCredentialConfig credential) {
    return AwsKmsCredentialSpecAssumeIAMDTO.builder().delegateSelectors(credential.getDelegateSelectors()).build();
  }

  private AwsKmsCredentialSpecAssumeSTSDTO buildFromStsConfig(AwsKmsStsCredentialConfig credential) {
    return AwsKmsCredentialSpecAssumeSTSDTO.builder()
        .delegateSelectors(credential.getDelegateSelectors())
        .roleArn(credential.getRoleArn())
        .externalName(credential.getExternalName())
        .assumeStsRoleDuration(credential.getAssumeStsRoleDuration())
        .build();
  }
}