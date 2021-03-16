package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import gov.va.api.health.ids.client.Format.TwoWayFormat;
import java.util.List;
import java.util.regex.Pattern;

public class PatientIcnFormat {

  /** Return a two way format that matches patient ICNs against the given pattern. */
  public static Format of(String patientIdPattern) {
    return TwoWayFormat.builder()
        .lookupHandler(PatientIcnLookupHandler.of(patientIdPattern))
        .registrationHandler(new PatientRegistrationHandler())
        .build();
  }

  /** Create an MVI PATIENT identity. */
  public static ResourceIdentity identityFor(String patientId) {
    return ResourceIdentity.builder()
        .system("MVI")
        .resource("PATIENT")
        .identifier(patientId)
        .build();
  }

  /**
   * This handler is used deal with IDs that match the 10V6 patient ICN pattern. ICNs should not be
   * encoded or decoded.
   */
  static class PatientIcnLookupHandler implements LookupHandler {

    private final Pattern icnPattern;

    PatientIcnLookupHandler(String patientIdPattern) {
      icnPattern = Pattern.compile(patientIdPattern);
    }

    public static PatientIcnLookupHandler of(String patientIdPattern) {
      return new PatientIcnLookupHandler(patientIdPattern);
    }

    @Override
    public boolean accept(String id) {
      return icnPattern.matcher(id).matches();
    }

    @Override
    public List<ResourceIdentity> lookup(String id) {
      return List.of(identityFor(id));
    }
  }

  /**
   * This handler will transparently register Patient ICNs. The system will always be `MVI` and the
   * resource will be `PATIENT`.
   */
  static class PatientRegistrationHandler implements RegistrationHandler {

    @Override
    public boolean accept(ResourceIdentity identity) {
      return "patient".equalsIgnoreCase(identity.resource());
    }

    @Override
    public Registration register(ResourceIdentity identity) {
      return Registration.builder()
          .uuid(identity.identifier())
          .resourceIdentities(List.of(identityFor(identity.identifier())))
          .build();
    }
  }
}
