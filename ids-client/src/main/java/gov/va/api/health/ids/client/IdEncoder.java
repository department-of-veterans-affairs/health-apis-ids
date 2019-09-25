package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.ResourceIdentity;

/** The ID encoder is responsible for managing the encoding and decoding of resource identities. */
public interface IdEncoder {
  ResourceIdentity decode(String encoded);

  String encode(ResourceIdentity resourceIdentity);
}
