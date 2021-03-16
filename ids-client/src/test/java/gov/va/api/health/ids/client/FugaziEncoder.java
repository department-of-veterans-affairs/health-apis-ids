package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.StringJoiner;

class FugaziEncoder implements IdEncoder {

  @Override
  public ResourceIdentity decode(String encoded) {
    String[] parts = encoded.split("-");
    return ResourceIdentity.builder()
        .system(parts[0])
        .resource(parts[1])
        .identifier(parts[2])
        .build();
  }

  @Override
  public String encode(ResourceIdentity resourceIdentity) {
    return new StringJoiner("-")
        .add(resourceIdentity.system())
        .add(resourceIdentity.resource())
        .add(resourceIdentity.identifier())
        .toString();
  }
}
