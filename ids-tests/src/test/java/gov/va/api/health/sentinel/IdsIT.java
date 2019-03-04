package gov.va.api.health.sentinel;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.sentinel.categories.Local;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IdsIT {
  private TestClient client = TestClients.ids();

  private String apiPath = client.service().apiPath();

  @Test
  @Category(Local.class)
  public void idsIsHealthy() {
    TestClients.ids().get("/actuator/health").response().then().body("status", equalTo("UP"));
  }

  @Test
  @Category(Local.class)
  public void legacyApiSupportedForOldMuleApplications() {
    ResourceIdentity identity =
        ResourceIdentity.builder()
            .system("CDW")
            .resource("WHATEVER")
            .identifier("whatever")
            .build();

    List<Registration> registrations =
        client
            .post(apiPath + "resourceIdentity", singletonList(identity))
            .expect(201)
            .expectListOf(Registration.class);
    assertThat(registrations.size()).isEqualTo(1);

    List<ResourceIdentity> identities =
        client
            .get(apiPath + "resourceIdentity/{id}", registrations.get(0).uuid())
            .expect(200)
            .expectListOf(ResourceIdentity.class);

    assertThat(identities).containsExactly(identity);
  }

  @Test
  @Category(Local.class)
  public void lookupReturns404ForUnknownId() {
    client.get(apiPath + "v1/ids/{id}", UUID.randomUUID().toString()).expect(404);
  }

  @Test
  @Category(Local.class)
  public void registerFlow() {
    ResourceIdentity identity =
        ResourceIdentity.builder()
            .system("CDW")
            .resource("WHATEVER")
            .identifier("whatever")
            .build();

    List<Registration> registrations =
        client
            .post(apiPath + "v1/ids", singletonList(identity))
            .expect(201)
            .expectListOf(Registration.class);
    assertThat(registrations.size()).isEqualTo(1);

    List<Registration> repeatedRegistrations =
        client
            .post(apiPath + "v1/ids", singletonList(identity))
            .expect(201)
            .expectListOf(Registration.class);
    assertThat(repeatedRegistrations).isEqualTo(registrations);

    List<ResourceIdentity> identities =
        client
            .get(apiPath + "v1/ids/{id}", registrations.get(0).uuid())
            .expect(200)
            .expectListOf(ResourceIdentity.class);

    assertThat(identities).containsExactly(identity);
  }

  @Test
  @Category(Local.class)
  public void registerPatientFlowUsesPatientProvidedIdentifier() {
    String icn = "185601V825290";
    ResourceIdentity identity =
        ResourceIdentity.builder().system("CDW").resource("PATIENT").identifier(icn).build();

    List<Registration> registrations =
        client
            .post(apiPath + "v1/ids", singletonList(identity))
            .expect(201)
            .expectListOf(Registration.class);
    assertThat(registrations.size()).isEqualTo(1);

    List<ResourceIdentity> identities =
        client.get(apiPath + "v1/ids/{id}", icn).expect(200).expectListOf(ResourceIdentity.class);

    assertThat(identities).containsExactly(identity);
  }

  @Test
  @Category(Local.class)
  public void registerReturns400ForInvalidRequest() {
    ResourceIdentity identity =
        ResourceIdentity.builder().system("CDW").resource("WHATEVER").identifier(null).build();
    client.post(apiPath + "v1/ids", singletonList(identity)).expect(400);
  }
}
