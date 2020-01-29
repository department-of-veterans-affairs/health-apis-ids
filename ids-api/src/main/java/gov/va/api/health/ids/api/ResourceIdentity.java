package gov.va.api.health.ids.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Schema(
    description =
        "A identifier for a particular resource type within a specific system."
            + " Identifiers are only unique for a given resource type and may be used within"
            + " the system for other resource types.")
public class ResourceIdentity {
  @Schema(description = "Unique identifier for the resource type in the given system.")
  @NotBlank
  String identifier;

  @Schema(description = "The system that defines the identifier.")
  @NotBlank
  String system;

  @Schema(description = "A resource type narrows the identifier within the system.")
  @NotBlank
  @Pattern(regexp = "[-A-Za-z0-9_]+")
  String resource;
}
