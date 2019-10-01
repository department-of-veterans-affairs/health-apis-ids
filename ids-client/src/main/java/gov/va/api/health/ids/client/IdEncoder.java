package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.ResourceIdentity;

/** The ID encoder is responsible for managing the encoding and decoding of resource identities. */
public interface IdEncoder {
  ResourceIdentity decode(String encoded);

  String encode(ResourceIdentity resourceIdentity);

  /**
   * BadId can happen fairly easily since IDs are provided by callers. This indicates the IDs could
   * not be decoded for any reason.
   */
  class BadId extends EncoderException {
    public BadId(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Parent for all IdEncoder exceptions. */
  class EncoderException extends RuntimeException {
    EncoderException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * EncodingFailed exception most likely mean that the IdEncoder implementation has not been
   * properly configured.
   */
  class EncodingFailed extends EncoderException {
    public EncodingFailed(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
