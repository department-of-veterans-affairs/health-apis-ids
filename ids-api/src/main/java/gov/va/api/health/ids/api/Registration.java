package gov.va.api.health.ids.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Registration {
  @NotBlank String uuid;
  @Singular @Valid List<ResourceIdentity> resourceIdentities;
}
