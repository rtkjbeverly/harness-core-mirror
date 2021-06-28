package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.entities.Token;
import io.harness.ng.core.entities.Token.TokenBuilder;

import java.time.Instant;
import java.util.Date;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class TokenDTOMapper {
  public Token getTokenFromDTO(TokenDTO dto, Long defaultTimeout) {
    TokenBuilder tokenBuilder = Token.builder()
                                    .identifier(dto.getIdentifier())
                                    .name(dto.getName())
                                    .apiKeyIdentifier(dto.getApiKeyIdentifier())
                                    .parentIdentifier(dto.getParentIdentifier())
                                    .apiKeyType(dto.getApiKeyType())
                                    .accountIdentifier(dto.getAccountIdentifier())
                                    .orgIdentifier(dto.getOrgIdentifier())
                                    .projectIdentifier(dto.getProjectIdentifier());
    Instant validFrom = dto.getValidFrom() != null ? Instant.ofEpochMilli(dto.getValidFrom()) : Instant.now();
    Instant validTo =
        dto.getValidTo() != null ? Instant.ofEpochMilli(dto.getValidTo()) : validFrom.plusMillis(defaultTimeout);
    tokenBuilder.validFrom(validFrom);
    tokenBuilder.validTo(validTo);
    if (dto.getScheduledExpireTime() != null) {
      tokenBuilder.scheduledExpireTime(Instant.ofEpochMilli(dto.getScheduledExpireTime()));
    }
    Token token = tokenBuilder.build();
    token.setValidUntil(new Date(token.getExpiryTimestamp().toEpochMilli()));
    return token;
  }

  public TokenDTO getDTOFromTokenForRotation(Token token) {
    return TokenDTO.builder()
        .identifier(token.getIdentifier())
        .name(token.getName())
        .validFrom(token.getScheduledExpireTime().toEpochMilli())
        .validTo(token.getValidTo().toEpochMilli())
        .apiKeyIdentifier(token.getApiKeyIdentifier())
        .parentIdentifier(token.getParentIdentifier())
        .apiKeyType(token.getApiKeyType())
        .accountIdentifier(token.getAccountIdentifier())
        .orgIdentifier(token.getOrgIdentifier())
        .projectIdentifier(token.getProjectIdentifier())
        .build();
  }

  public TokenDTO getDTOFromToken(Token token) {
    return TokenDTO.builder()
        .identifier(token.getIdentifier())
        .name(token.getName())
        .validFrom(token.getValidFrom().toEpochMilli())
        .validTo(token.getValidTo().toEpochMilli())
        .apiKeyIdentifier(token.getApiKeyIdentifier())
        .parentIdentifier(token.getParentIdentifier())
        .apiKeyType(token.getApiKeyType())
        .accountIdentifier(token.getAccountIdentifier())
        .orgIdentifier(token.getAccountIdentifier())
        .projectIdentifier(token.getProjectIdentifier())
        .scheduledExpireTime(
            token.getScheduledExpireTime() != null ? token.getScheduledExpireTime().toEpochMilli() : null)
        .valid(token.isValid())
        .build();
  }
}