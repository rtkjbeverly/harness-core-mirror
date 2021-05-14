package io.harness.licensing.api.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/admin/licenses")
@Path("/admin/licenses")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class AdminLicenseResource {
  private static final String MODULE_TYPE_KEY = "moduleType";
  private final LicenseService licenseService;

  @Inject
  public AdminLicenseResource(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @GET
  @Path("account")
  @InternalApi
  public ResponseDTO<AccountLicensesDTO> getAccountLicensesDTO(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier) {
    // TODO change to Admin Auth when it's ready
    AccountLicensesDTO accountLicenses = licenseService.getAccountLicense(accountIdentifier);
    return ResponseDTO.newResponse(accountLicenses);
  }

  @POST
  @Path("{identifier}")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> create(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @PathParam("identifier") String identifier, @NotNull @Valid ModuleLicenseDTO moduleLicenseDTO) {
    // TODO change to Admin Auth when it's ready
    ModuleLicenseDTO created = licenseService.createModuleLicense(moduleLicenseDTO);
    return ResponseDTO.newResponse(created);
  }

  @PUT
  @Path("{identifier}")
  @InternalApi
  public ResponseDTO<ModuleLicenseDTO> update(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @PathParam("identifier") String identifier, @NotNull @Valid ModuleLicenseDTO moduleLicenseDTO) {
    // TODO change to Admin Auth when it's ready
    ModuleLicenseDTO updated = licenseService.updateModuleLicense(moduleLicenseDTO);
    return ResponseDTO.newResponse(updated);
  }
}