package gov.va.api.health.ids.client;

import static java.util.stream.Collectors.toList;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import gov.va.api.health.ids.client.IdEncoder.BadId;
import java.util.List;
import java.util.Optional;
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
 *   <li>V2 IDs are prefixed with 'I2-'
 *   <li>Patient ICN are never encoded and are recognized by the 10V6 pattern: ten digits, followed
 *       by a literal 'V', followed by 6 digits. Patients are always registered with the system
 *       'MVI' and the public 'uuid' will be ICN.
 *   <li>If a UUID is looked up, this implementation will delegate the lookup to a secondary client,
 *       e.g. The RestIdentityServiceClient
 * </ul>
 */
@Getter
public class EncodingIdentityServiceClient implements IdentityService {

  /**
   * This is an ordered list of formats. On lookup, each format will be evaluated to see if it will
   * accept the ID. The first format to do so will be used.
   */
  private final List<Format> formats;

  /** Construct a client with standard handlers. */
  @Builder
  public EncodingIdentityServiceClient(
      IdentityService delegate, IdEncoder encoder, String patientIdPattern) {
    this(
        List.of(
            PatientIcnFormat.of(patientIdPattern),
            EncodedIdFormat.of(EncodedIdFormat.V2_PREFIX, encoder),
            UuidFormat.of(delegate)));
  }

  public EncodingIdentityServiceClient(List<Format> formats) {
    this.formats = formats;
  }

  @Override
  public List<ResourceIdentity> lookup(String id) {
    LookupHandler handler =
        formats().stream()
            .map(Format::lookupHandler)
            .filter(h -> h.accept(id))
            .findFirst()
            .orElseThrow(() -> new BadId("Do not understand id: " + id));
    return handler.lookup(id);
  }

  @Override
  public List<Registration> register(List<ResourceIdentity> identities) {
    return identities.stream().map(this::register).collect(toList());
  }

  /** Register a single identity. */
  private Registration register(ResourceIdentity identity) {
    RegistrationHandler handler =
        formats.stream()
            .map(Format::registrationHandler)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(h -> h.accept(identity))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Failed to find registration handler:" + identity));
    return handler.register(identity);
  }
}
