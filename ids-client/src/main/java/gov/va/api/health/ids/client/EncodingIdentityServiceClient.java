package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * The EncodingIdentityServiceClient provides a mechanism to transition from a database-backed
 * system to a completely encoded ID. The encoded IDs contain the system,resource, and private ID in
 * an form that allows two translation. That is, it can be encoded then later decoded. However, to
 * support in-flight IDs based on the old system, the client will recognize the new style encoded
 * IDs and previous version which used UUIDs. This client behaves as follows:
 *
 * <ul>
 *   <li>IDs are always registered using the V2 mechanism.
 *   <li>V2 IDs are prefixed with 'i2:'
 *   <li>Patient ICN are never encoded and are recognized by the 10V6 pattern: ten digits, followed
 *       by a literal 'V', followed by 6 digits. Patients are always registered with the system
 *       'MVI' and the public 'uuid' will be ICN.
 *   <li>If a UUID is looked up, this implementation will delegate the lookup to a secondary client,
 *       e.g. The RestIdentityServiceClient
 * </ul>
 */
@Builder
@Getter
public class EncodingIdentityServiceClient implements IdentityService {

  /**
   * This prefix is the starting marker of V2 ids. IDs that start with this string will be passed to
   * the encoder for decoding. All ids generated during registration will have this prefix.
   */
  public static final String V2_PREFIX = "i2:";

  /**
   * UUID type IDs will be passed to this delegate for lookups. It will not be used for
   * registration.
   */
  private final IdentityService delegate;

  /** This is used for registration of all IDs and lookup if encoded IDs. */
  private final IdEncoder encoder;

  /**
   * This is an ordered list of handlers. On lookup, each handler will be evaluated to see if it
   * will accept the ID. The first handler to do so will be used. The "Unknown" handler should be
   * last in this list so that it can be used as a fallback.
   */
  private final List<LookupHandler> lookupHandlers =
      List.of(
          new PatientIcnLookupHandler(),
          new V2LookupHandler(),
          new UuidLookupHandler(),
          new UnknownFormatLookupHandler());

  private final List<RegistrationHandler> registrationHandlers =
      List.of(new PatientRegistrationHandler(), new V2RegistrationHandler());

  @Override
  public List<ResourceIdentity> lookup(String id) {
    LookupHandler handler =
        lookupHandlers()
            .stream()
            .filter(h -> h.accept(id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to find lookup handler: " + id));
    return handler.lookup(id);
  }

  @Override
  public List<Registration> register(List<ResourceIdentity> identities) {
    return identities.stream().map(this::register).collect(Collectors.toList());
  }

  /** Register a single identity. */
  private Registration register(ResourceIdentity identity) {
    RegistrationHandler handler =
        registrationHandlers()
            .stream()
            .filter(h -> h.accept(identity))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Failed to find registration handler:" + identity));
    return handler.register(identity);
  }

  /**
   * Registration handlers encapsulate the logic necessary to register different types of resources.
   */
  private interface RegistrationHandler {
    /** Return true if this handler understands the given identity. */
    boolean accept(ResourceIdentity identity);

    /**
     * Perform registration. This is only called if the handler indicates it accepts the identity.
     */
    Registration register(ResourceIdentity identity);
  }

  /** Lookup handlers encapsulate the logic necessary to perform lookups for each ID type. */
  private interface LookupHandler {
    /** Return true if this handler understands the given ID. */
    boolean accept(String id);

    /** This is only called if the handler indicates that it can accept it. */
    List<ResourceIdentity> lookup(String id);
  }

  /**
   * This handler is used deal with IDs that match the 10V6 patient ICN pattern. ICNs should not be
   * encoded or decoded.
   */
  private static class PatientIcnLookupHandler implements LookupHandler {

    private final Pattern icnPattern = Pattern.compile("[0-9]{10}V[0-9]{6}");

    @Override
    public boolean accept(String id) {
      return icnPattern.matcher(id).matches();
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      return List.of(
          ResourceIdentity.builder().system("MVI").resource("PATIENT").identifier(id).build());
    }
  }

  /**
   * This handler will transparently register Patient ICNs. The system will always be `MVI` and the
   * resource will be `PATIENT`.
   */
  private static class PatientRegistrationHandler implements RegistrationHandler {

    @Override
    public boolean accept(ResourceIdentity identity) {
      return "patient".equalsIgnoreCase(identity.resource());
    }

    @Override
    public Registration register(ResourceIdentity identity) {
      return Registration.builder()
          .uuid(identity.identifier())
          .resourceIdentity(
              ResourceIdentity.builder()
                  .system("MVI")
                  .resource("PATIENT")
                  .identifier(identity.identifier())
                  .build())
          .build();
    }
  }

  /**
   * This handler is used deal with IDs that do not match a known format. This should be the last
   * handler in the list of handlers.
   */
  private static class UnknownFormatLookupHandler implements LookupHandler {

    @Override
    public boolean accept(String id) {
      return true;
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      return List.of(
          ResourceIdentity.builder().system("UNKNOWN").resource("UNKNOWN").identifier(id).build());
    }
  }

  /** This handler will emit encoded V2 ids. */
  private class V2RegistrationHandler implements RegistrationHandler {

    @Override
    public boolean accept(ResourceIdentity identity) {
      return true;
    }

    @Override
    public Registration register(ResourceIdentity identity) {
      return Registration.builder()
          .uuid(V2_PREFIX + encoder().encode(identity))
          .resourceIdentity(identity.toBuilder().build())
          .build();
    }
  }

  /** This handler understands UUID and will delegate lookups. */
  private class UuidLookupHandler implements LookupHandler {

    private final Pattern uuidPattern =
        Pattern.compile(
            "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean accept(String id) {
      return uuidPattern.matcher(id).matches();
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      return delegate().lookup(id);
    }
  }

  /** This handler uses the encoder to decode V2 style IDs. */
  private class V2LookupHandler implements LookupHandler {

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
}
