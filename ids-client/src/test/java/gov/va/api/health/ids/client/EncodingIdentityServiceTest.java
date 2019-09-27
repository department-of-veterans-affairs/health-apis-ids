package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EncodingIdentityServiceTest {

  @Mock IdentityService delegate;

  private EncodingIdentityServiceClient ids;

  @Before
  public void _init() {
    MockitoAnnotations.initMocks(this);
    ids =
        EncodingIdentityServiceClient.builder()
            .encoder(new FugaziEncoder())
            .delegate(delegate)
            .build();
  }

  private ResourceIdentity anything(String id) {
    return ResourceIdentity.builder().system("CDW").resource("ANYTHING").identifier(id).build();
  }

  @Test
  public void icnsAreAlwaysReturnedForPatientsDuringRegistration() {
    /* For registration (encoding), full ICNs are returned for Patient resources. */
    List<Registration> actual = ids.register(List.of(patient("1011537977V693883"), patient("123")));
    assertThat(actual)
        .containsExactlyInAnyOrder(
            registration("123", patient("123")),
            registration("1011537977V693883", patient("1011537977V693883")));
  }

  @Test
  public void icnsAreDetectedAndMviTupleIsReturned() {
    /*
     * For lookup, V2 ID management will detect full ICN and return tuple of system = MVI, resource
     * = PATIENT, identity = ${icn}
     */
    List<ResourceIdentity> actual = ids.lookup("1011537977V693883");
    assertThat(actual).isEqualTo(List.of(patient("1011537977V693883")));
  }

  @Test
  public void idsWithUnknownFormatsAreReturnedAsUnknownTuple() {
    /*
     * For lookup, V2 ID management will return a tuple of system = UNKNOWN, resource = UNKNOWN,
     * identity = ${lookup-value}, when the look up value is not a full ICN, V1 ID (type 5 UUID), or
     * V2 ID.
     */
    String mysteryId = "12345";
    List<ResourceIdentity> actual = ids.lookup(mysteryId);
    assertThat(actual).isEqualTo(List.of(unknown("12345")));
  }

  private ResourceIdentity patient(String icn) {
    return ResourceIdentity.builder().system("MVI").resource("PATIENT").identifier(icn).build();
  }

  private Registration registration(String uuid, ResourceIdentity ri) {
    return Registration.builder().uuid(uuid).resourceIdentity(ri).build();
  }

  private ResourceIdentity unknown(String id) {
    return ResourceIdentity.builder().system("UNKNOWN").resource("UNKNOWN").identifier(id).build();
  }

  @Test
  public void uuidsArePassedToDelegateService() {
    /*
     * For lookup, V2 ID management will detect V1 IDs (type 5 UUID) and delegate using the current
     * logic to the Identity Service.
     */
    String uuid = "b5f5682c-df90-11e9-8a34-2a2ae2dbcce4";
    when(delegate.lookup(uuid)).thenReturn(List.of(anything("1")));
    List<ResourceIdentity> actual = ids.lookup(uuid);
    assertThat(actual).isEqualTo(List.of(anything("1")));
  }

  @Test
  public void v2IdsAreAlwaysReturnedExceptForPatientsDuringRegistration() {
    /*
     * V2 IDs take the format described below, e.g.
     * I2-n%2BtRVIJFNsJfd1FXLD0U8s9n3Fen5MLCa8RztnXC9mY%3D
     *
     * A simple encryption algorithm such as AES-128 will be used to keep ID sizes from becoming
     * excessively large
     *
     * For registration (encoding) V2 IDs are always emitted except for Patient resources.
     */
    List<Registration> actual =
        ids.register(List.of(patient("1234567890V123456"), anything("a"), anything("b")));
    assertThat(actual).hasSize(3);
    assertThat(actual.get(0))
        .matches(
            ExpectedRegistration.withId()
                .system("MVI")
                .resource("PATIENT")
                .privateId("1234567890V123456")
                .publicId("1234567890V123456")
                .build());
    assertThat(actual.get(1))
        .matches(
            ExpectedRegistration.withEncodedId()
                .system("CDW")
                .resource("ANYTHING")
                .privateId("a")
                .build());
    assertThat(actual.get(2))
        .matches(
            ExpectedRegistration.withEncodedId()
                .system("CDW")
                .resource("ANYTHING")
                .privateId("b")
                .build());
  }

  @Test
  public void v2IdsAreDecoded() {
    /* For lookup, V2 ID management will detect V2 IDs and decode accordingly. */
    String encoded =
        EncodingIdentityServiceClient.V2_PREFIX + new FugaziEncoder().encode(anything("abc"));
    List<ResourceIdentity> actual = ids.lookup(encoded);
    assertThat(actual).containsExactly(anything("abc"));
  }

  @Value
  private static class ExpectedRegistration implements Predicate<Registration> {
    String system;
    String resource;
    String privateId;
    Predicate<String> publicId;

    @Builder(
        builderMethodName = "withEncodedId",
        builderClassName = "ExpectedRegistrationBuilderWithEncodedId")
    public ExpectedRegistration(String system, String resource, String privateId) {
      this.system = system;
      this.resource = resource;
      this.privateId = privateId;
      publicId = id -> id.matches(EncodingIdentityServiceClient.V2_PREFIX + "[-_a-zA-Z0-9]+");
    }

    @Builder(builderMethodName = "withId", builderClassName = "ExpectedRegistrationBuilderWithId")
    public ExpectedRegistration(String system, String resource, String privateId, String publicId) {
      this.system = system;
      this.resource = resource;
      this.privateId = privateId;
      this.publicId = id -> publicId.equals(id);
    }

    @Override
    public boolean test(Registration registration) {
      return publicId.test(registration.uuid())
          && registration.resourceIdentities().size() == 1
          && registration
              .resourceIdentities()
              .get(0)
              .equals(
                  ResourceIdentity.builder()
                      .system(system)
                      .resource(resource)
                      .identifier(privateId)
                      .build());
    }
  }

  static class FugaziEncoder implements IdEncoder {

    @Override
    public ResourceIdentity decode(String encoded) {
      String[] parts = encoded.split("-");
      return ResourceIdentity.builder()
          .system(parts[0])
          .resource(parts[1])
          .identifier(parts[2])
          .build();
    }

    @Override
    public String encode(ResourceIdentity resourceIdentity) {
      return new StringJoiner("-")
          .add(resourceIdentity.system())
          .add(resourceIdentity.resource())
          .add(resourceIdentity.identifier())
          .toString();
    }
  }
}
