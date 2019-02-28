package gov.va.api.health.ids.service.controller.impl;

import com.fasterxml.uuid.Generators;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.service.controller.IdServiceV1ApiController.UuidGenerator;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates a UUID in a deterministic approach using a seed UUID plus the resource type and
 * identifier.
 */
@NoArgsConstructor
@AllArgsConstructor
@Service
public class Type5UuidGenerator implements UuidGenerator {

  @Value("${uuid.seed}")
  private String seed;

  @Override
  public String apply(@NonNull ResourceIdentity resourceIdentity) {
    if (isSpecialPatient(resourceIdentity)) {
      return resourceIdentity.identifier();
    }
    return Generators.nameBasedGenerator(UUID.fromString(seed))
        .generate(resourceIdentity.resource() + ":" + resourceIdentity.identifier())
        .toString();
  }

  /** Checks to see if resource is "PATIENT" and system is "CDW". */
  boolean isSpecialPatient(@NonNull ResourceIdentity resourceIdentity) {
    return "CDW".equals(resourceIdentity.system()) && "PATIENT".equals(resourceIdentity.resource());
  }
}
