package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import gov.va.api.health.ids.client.Format.TwoWayFormat;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class EncodedIdFormat {
  /**
   * This prefix is the starting marker of V2 ids. IDs that start with this string will be passed to
   * the encoder for decoding. All ids generated during registration will have this prefix.
   */
  public static final String V2_PREFIX = "I2-";

  public static final String V3_PREFIX = "I3-";

  /** Return a two way format that leverages the given encoder. */
  public static Format of(String prefix, IdEncoder encoder) {
    return TwoWayFormat.builder()
        .lookupHandler(V2LookupHandler.builder().prefix(prefix).encoder(encoder).build())
        .registrationHandler(
            V2RegistrationHandler.builder().prefix(prefix).encoder(encoder).build())
        .build();
  }

  /** This handler uses the encoder to decode V2 style IDs. */
  @Builder
  public static class V2LookupHandler implements LookupHandler {

    /** This is used for registration of all IDs and lookup if encoded IDs. */
    @Getter private final IdEncoder encoder;

    @Getter private final String prefix;

    @Override
    public boolean accept(String id) {
      return id.startsWith(prefix());
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      String unversionedId = id.substring(prefix().length());
      return List.of(encoder().decode(unversionedId));
    }
  }

  /** This handler will emit encoded V2 ids. */
  @Builder
  public static class V2RegistrationHandler implements RegistrationHandler {

    /** This is used for registration of all IDs and lookup if encoded IDs. */
    @Getter private final IdEncoder encoder;

    @Getter private final String prefix;

    @Override
    public boolean accept(ResourceIdentity identity) {
      return true;
    }

    @Override
    public Registration register(ResourceIdentity identity) {
      return Registration.builder()
          .uuid(prefix() + encoder().encode(identity))
          .resourceIdentities(List.of(identity.toBuilder().build()))
          .build();
    }
  }
}
