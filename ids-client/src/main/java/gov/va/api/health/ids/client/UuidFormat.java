package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.LookupOnlyFormat;
import java.util.List;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

/**
 * Lookup only format that uses a dedicate IdentityService (usually external) to perform lookups.
 */
@UtilityClass
public class UuidFormat {

  public static Format of(IdentityService delegate) {
    return LookupOnlyFormat.builder().lookupHandler(UuidLookupHandler.of(delegate)).build();
  }

  /** This handler understands UUID and will delegate lookups. */
  @AllArgsConstructor(staticName = "of")
  public static class UuidLookupHandler implements LookupHandler {

    /**
     * UUID type IDs will be passed to this delegate for lookups. It will not be used for
     * registration.
     */
    @Getter private final IdentityService delegate;

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
}
