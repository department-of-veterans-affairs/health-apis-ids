package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import gov.va.api.health.ids.client.IdEncoder.BadId;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientIcnLookupHandler;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientRegistrationHandler;
import gov.va.api.health.ids.client.UuidFormat.UuidLookupHandler;
import gov.va.api.health.ids.client.V2Format.V2LookupHandler;
import gov.va.api.health.ids.client.V2Format.V2RegistrationHandler;
import java.util.List;
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
   * This is an ordered list of handlers. On lookup, each handler will be evaluated to see if it
   * will accept the ID. The first handler to do so will be used.
   */
  private final List<LookupHandler> lookupHandlers;

  private final List<RegistrationHandler> registrationHandlers;

  /** Construct a client with standard handlers. */
  @Builder
  public EncodingIdentityServiceClient(
      IdentityService delegate, IdEncoder encoder, String patientIdPattern) {
    lookupHandlers =
        List.of(
            PatientIcnLookupHandler.of(patientIdPattern),
            V2LookupHandler.of(encoder),
            UuidLookupHandler.of(delegate));
    registrationHandlers =
        List.of(new PatientRegistrationHandler(), V2RegistrationHandler.of(encoder));
  }

  @Override
  public List<ResourceIdentity> lookup(String id) {
    LookupHandler handler =
        lookupHandlers().stream()
            .filter(h -> h.accept(id))
            .findFirst()
            .orElseThrow(() -> new BadId("Do not understand id: " + id));
    return handler.lookup(id);
  }

  @Override
  public List<Registration> register(List<ResourceIdentity> identities) {
    return identities.stream().map(this::register).collect(Collectors.toList());
  }

  /** Register a single identity. */
  private Registration register(ResourceIdentity identity) {
    RegistrationHandler handler =
        registrationHandlers().stream()
            .filter(h -> h.accept(identity))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Failed to find registration handler:" + identity));
    return handler.register(identity);
  }
}
