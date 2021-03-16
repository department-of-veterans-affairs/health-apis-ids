package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.List;

public interface Format {

  /**
   * Registration handlers encapsulate the logic necessary to register different types of resources.
   */
  interface RegistrationHandler {
    /** Return true if this handler understands the given identity. */
    boolean accept(ResourceIdentity identity);

    /**
     * Perform registration. This is only called if the handler indicates it accepts the identity.
     */
    Registration register(ResourceIdentity identity);
  }

  /** Lookup handlers encapsulate the logic necessary to perform lookups for each ID type. */
  interface LookupHandler {
    /** Return true if this handler understands the given ID. */
    boolean accept(String id);

    /** This is only called if the handler indicates that it can accept it. */
    List<ResourceIdentity> lookup(String id);
  }
}
