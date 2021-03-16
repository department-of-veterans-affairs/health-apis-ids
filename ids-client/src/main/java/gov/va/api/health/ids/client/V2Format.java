package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class V2Format {
  /**
   * This prefix is the starting marker of V2 ids. IDs that start with this string will be passed to
   * the encoder for decoding. All ids generated during registration will have this prefix.
   */
  public static final String V2_PREFIX = "I2-";

  /** This handler uses the encoder to decode V2 style IDs. */
  @AllArgsConstructor(staticName = "of")
  public static class V2LookupHandler implements LookupHandler {

    /** This is used for registration of all IDs and lookup if encoded IDs. */
    @Getter private final IdEncoder encoder;

    @Override
    public boolean accept(String id) {
      return id.startsWith(V2_PREFIX);
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      String unversionedId = id.substring(V2_PREFIX.length());
      return List.of(encoder().decode(unversionedId));
    }
  }

  /** This handler will emit encoded V2 ids. */
  @AllArgsConstructor(staticName = "of")
  public static class V2RegistrationHandler implements RegistrationHandler {

    /** This is used for registration of all IDs and lookup if encoded IDs. */
    @Getter private final IdEncoder encoder;

    @Override
    public boolean accept(ResourceIdentity identity) {
      return true;
    }

    @Override
    public Registration register(ResourceIdentity identity) {
      return Registration.builder()
          .uuid(V2_PREFIX + encoder().encode(identity))
          .resourceIdentities(List.of(identity.toBuilder().build()))
          .build();
    }
  }
}
