package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketDTOToEntity;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.ng.userprofile.commons.BitbucketSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "BitBucketSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.BitbucketSCM")
public class BitbucketSCM extends SourceCodeManager {
  GitAuthType authType;
  BitbucketAuthentication authenticationDetails;

  @Override
  public SCMType getType() {
    return SCMType.BITBUCKET;
  }

  public static class BitbucketSCMMapper extends SourceCodeManagerMapper<BitbucketSCMDTO, BitbucketSCM> {
    @Override
    public BitbucketSCM toSCMEntity(BitbucketSCMDTO sourceCodeManagerDTO) {
      BitbucketSCM bitbucketSCM = BitbucketSCM.builder()
                                      .authType(sourceCodeManagerDTO.getBitbucketAuthenticationDTO().getAuthType())
                                      .authenticationDetails(BitbucketDTOToEntity.buildAuthenticationDetails(
                                          sourceCodeManagerDTO.getBitbucketAuthenticationDTO().getAuthType(),
                                          sourceCodeManagerDTO.getBitbucketAuthenticationDTO().getCredentials()))
                                      .build();
      setCommonFieldsEntity(bitbucketSCM, sourceCodeManagerDTO);
      return bitbucketSCM;
    }

    @Override
    public BitbucketSCMDTO toSCMDTO(BitbucketSCM sourceCodeManager) {
      BitbucketSCMDTO bitbucketSCMDTO =
          BitbucketSCMDTO.builder()
              .bitbucketAuthenticationDTO(BitbucketEntityToDTO.buildBitbucketAuthentication(
                  sourceCodeManager.getAuthType(), sourceCodeManager.getAuthenticationDetails()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, bitbucketSCMDTO);
      return bitbucketSCMDTO;
    }
  }
}