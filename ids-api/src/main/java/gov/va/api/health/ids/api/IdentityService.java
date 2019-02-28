package gov.va.api.health.ids.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@OpenAPIDefinition(
  info =
      @Info(
        title = "Identity Service",
        version = "v1",
        description = "Performs mapping from public IDs to internal system IDs."
      )
)
@Path("api/v1/ids")
public interface IdentityService {

  @Operation(
    summary = "Look up a resource identity",
    description =
        "Look up a resource identity using the public ID "
            + "determined from a previous registration."
  )
  @GET
  @Path("/{id}")
  @ApiResponse(
    responseCode = "200",
    description = "Resource identity found",
    content =
        @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = ResourceIdentity.class))
        )
  )
  @ApiResponse(responseCode = "400", description = "Not found")
  @ApiResponse(responseCode = "404", description = "Bad request")
  List<ResourceIdentity> lookup(
      @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            required = true,
            description = "The public UUD returned in a previous registration.",
            schema = @Schema(format = "uuid")
          )
          String id);

  @Operation(
    summary = "Register one or more identities",
    description =
        "Register resource identities. This operation is idempotent. "
            + "The returned registration UUID can be used to lookup resource identities."
  )
  @POST
  @RequestBody(
    description = "Resource identities to register",
    content =
        @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = ResourceIdentity.class))
        )
  )
  @ApiResponse(
    responseCode = "201",
    description = "Registration created",
    content =
        @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = Registration.class))
        )
  )
  @ApiResponse(responseCode = "404", description = "Bad request")
  List<Registration> register(List<ResourceIdentity> identities);

  class IdentityServiceException extends RuntimeException {
    IdentityServiceException(String message) {
      super(message);
    }
  }

  class UnknownIdentity extends IdentityServiceException {
    public UnknownIdentity(String id) {
      super(id);
    }
  }

  class LookupFailed extends IdentityServiceException {
    public LookupFailed(String id, String reason) {
      super(id + " Reason: " + reason);
    }
  }

  class RegistrationFailed extends IdentityServiceException {
    public RegistrationFailed(String message) {
      super(message);
    }
  }
}
