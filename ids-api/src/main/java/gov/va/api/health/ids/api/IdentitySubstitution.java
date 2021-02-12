package gov.va.api.health.ids.api;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class IdentitySubstitution<ReferenceT> {

  protected final IdentityService identityService;

  protected final Function<ReferenceT, Optional<ResourceIdentity>> toResourceIdentity;

  protected final Function<String, RuntimeException> throwWhenNotFound;

  /** Attempt to lookup the private ID for the given public ID and system. */
  public Optional<String> privateIdOf(@NonNull String system, @NonNull String publicId) {
    return identityService.lookup(publicId).stream()
        .filter(identity -> system.equals(identity.system()))
        .map(ResourceIdentity::identifier)
        .findFirst();
  }

  public <T> IdentityMapping register(
      Collection<T> resources, Function<T, Stream<ReferenceT>> referencesOf) {
    return registerAndMap(uniqueIdentitiesOf(resources, referencesOf));
  }

  /** Register IDs. */
  public List<Registration> register(Collection<ResourceIdentity> ids) {
    if (isEmpty(ids)) {
      return emptyList();
    }
    return identityService.register(new ArrayList<>(ids));
  }

  /** Register IDs and return an IdentityMapping that can be used to easily find public IDs. */
  public IdentityMapping registerAndMap(Collection<ResourceIdentity> ids) {
    return new IdentityMapping(register(ids));
  }

  /** Lookup and convert the given public ID to a ResourceIdentity. */
  public ResourceIdentity toResourceIdentity(String publicId) {
    return identityService.lookup(publicId).stream()
        .findFirst()
        .orElseThrow(
            () -> throwWhenNotFound.apply("Resource Identity " + publicId + " not found."));
  }

  /**
   * Return a set of unique resource identities derived from the collection of resources using the
   * given function. The function may return null entries, which will be filtered out..
   */
  protected <T> Set<ResourceIdentity> uniqueIdentitiesOf(
      Collection<T> resources, Function<T, Stream<ReferenceT>> referencesOf) {
    Set<ResourceIdentity> ids =
        resources.stream()
            .flatMap(referencesOf)
            .filter(Objects::nonNull)
            .map(toResourceIdentity)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    return ids;
  }

  public static class IdentityMapping {
    /** Mapping of resource name and private ID to registration. */
    private final Map<IdentityMappingKey, Registration> ids;

    /** Create a new instance with the given registrations. */
    public IdentityMapping(List<Registration> registrations) {
      ids = new HashMap<>();
      for (Registration r : registrations) {
        for (ResourceIdentity id : r.resourceIdentities()) {
          ids.put(IdentityMappingKey.of(id.resource(), id.identifier()), r);
        }
      }
    }

    /**
     * Return the mapping for the public ID of the resource and id if it exists. The resource name
     * should be in IdentityService format, e.g. DIAGNOSTIC_REPORT instead of "DiagnosticReport"
     */
    public Optional<String> publicIdOf(String resourceInIdentityServiceFormat, String privateId) {
      if (resourceInIdentityServiceFormat == null || privateId == null) {
        return Optional.empty();
      }
      Registration registration =
          ids.get(IdentityMappingKey.of(resourceInIdentityServiceFormat, privateId));
      return Optional.ofNullable(registration == null ? null : registration.uuid());
    }

    /**
     * For each resource provided, extract, filter, and update references with public IDs. This
     * method assumes that private IDs are held as a reference object. To accommodate that, a series
     * of operations are performed, defined by a group of functions.
     */
    public <T, ReferenceT> void replacePrivateIdsWithPublicIds(
        Collection<T> resources, Operations<T, ReferenceT> operations) {
      resources.stream()
          .flatMap(operations.toReferences())
          .filter(Objects::nonNull)
          .filter(operations.isReplaceable())
          .forEach(
              reference -> {
                var id =
                    publicIdOf(
                        operations.resourceNameOf().apply(reference),
                        operations.privateIdOf().apply(reference));
                if (id.isPresent()) {
                  operations.updatePrivateIdToPublicId().accept(reference, id);
                }
              });
    }
  }

  @Value
  @Builder
  @AllArgsConstructor(staticName = "of")
  public static class IdentityMappingKey {
    String resourceName;
    String privateId;
  }

  @Builder
  @Getter
  public static class Operations<ResourceT, ReferenceT> {
    @NonNull private final Function<ResourceT, Stream<ReferenceT>> toReferences;
    @NonNull private final Predicate<ReferenceT> isReplaceable;
    @NonNull private final Function<ReferenceT, String> resourceNameOf;
    @NonNull private final Function<ReferenceT, String> privateIdOf;
    @NonNull private final BiConsumer<ReferenceT, Optional<String>> updatePrivateIdToPublicId;
  }
}
