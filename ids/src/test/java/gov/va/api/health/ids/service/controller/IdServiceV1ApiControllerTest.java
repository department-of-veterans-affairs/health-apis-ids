package gov.va.api.health.ids.service.controller;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.IdentityService.UnknownIdentity;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.service.controller.IdServiceV1ApiController.UuidGenerator;
import gov.va.api.health.ids.service.controller.impl.ResourceIdentityDetail;
import gov.va.api.health.ids.service.controller.impl.ResourceIdentityDetailRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class IdServiceV1ApiControllerTest {
  @Mock ResourceIdentityDetailRepository repo;
  @Mock UuidGenerator uuidGenerator;
  private IdServiceV1ApiController controller;

  @BeforeEach
  public void _init() {
    MockitoAnnotations.initMocks(this);
    controller = new IdServiceV1ApiController(repo, uuidGenerator);
  }

  /**
   * What a detail will look like if it already exists in the database. The auto-incremented primary
   * key field 'pk' will be set.
   */
  private ResourceIdentityDetail existingDetail(int i) {
    return ResourceIdentityDetail.builder()
        .pk(i)
        .identifier("i" + i)
        .resource("r" + i)
        .system("s" + i)
        .uuid("x")
        .build();
  }

  @Test
  public void lookupReturns200AndIdentitiesWhenFound() {
    List<ResourceIdentityDetail> searchResults =
        asList(existingDetail(3), existingDetail(2), existingDetail(1));
    when(repo.findByUuid("x")).thenReturn(searchResults);

    ResponseEntity<List<ResourceIdentity>> actual = controller.lookup("x");

    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actual.getBody())
        .containsExactlyInAnyOrder(resourceIdentity(3), resourceIdentity(2), resourceIdentity(1));
  }

  @Test
  public void lookupThrowsUnknownIdentityExceptionWhenNoIdentitiesAreFound() {
    List<ResourceIdentityDetail> searchResults = new ArrayList<>();
    when(repo.findByUuid("x")).thenReturn(searchResults);

    assertThatExceptionOfType(UnknownIdentity.class).isThrownBy(() -> controller.lookup("x"));
  }

  /**
   * What a detail will look like if it does not exist in the database. The auto-incremented primary
   * key field 'pk' will be 0.
   */
  private ResourceIdentityDetail newDetail(String uuid, int i) {
    return ResourceIdentityDetail.builder()
        .pk(0)
        .identifier("i" + i)
        .resource("r" + i)
        .system("s" + i)
        .uuid(uuid)
        .build();
  }

  private Registration registration(String uuid, ResourceIdentity resourceIdentity) {
    return Registration.builder().uuid(uuid).resourceIdentities(List.of(resourceIdentity)).build();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void registrationForAlreadyRegisteredIdentitiesDoesNotRegisterTwice() {
    ResourceIdentity alreadyRegistered = resourceIdentity(1);
    ResourceIdentity notRegisteredYet = resourceIdentity(2);

    when(uuidGenerator.apply(notRegisteredYet)).thenReturn("u2");
    when(repo.findBySystemAndResourceAndIdentifier("s1", "r1", "i1"))
        .thenReturn(asList(existingDetail(1)));

    ResponseEntity<List<Registration>> registrationResult =
        controller.register(asList(alreadyRegistered, notRegisteredYet));

    assertThat(registrationResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(registrationResult.getBody())
        .containsExactlyInAnyOrder(
            registration("x", alreadyRegistered), registration("u2", notRegisteredYet));

    ArgumentCaptor<Iterable<ResourceIdentityDetail>> saveArgs =
        ArgumentCaptor.forClass(Iterable.class);
    verify(repo)
        .findBySystemAndResourceAndIdentifier(
            alreadyRegistered.system(),
            alreadyRegistered.resource(),
            alreadyRegistered.identifier());
    verify(repo)
        .findBySystemAndResourceAndIdentifier(
            notRegisteredYet.system(), notRegisteredYet.resource(), notRegisteredYet.identifier());
    verify(repo).saveAll(saveArgs.capture());
    assertThat(saveArgs.getValue()).containsExactly(newDetail("2", 2));
    verifyNoMoreInteractions(repo);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void registrationReturn201AndRegistrationsForUnregisteredId() {
    ResourceIdentity id1 = resourceIdentity(1);
    ResourceIdentity id2 = resourceIdentity(2);
    when(repo.findByUuid(Mockito.anyString())).thenReturn(Collections.emptyList());
    when(uuidGenerator.apply(id1)).thenReturn("1");
    when(uuidGenerator.apply(id2)).thenReturn("2");

    ArgumentCaptor<Iterable<ResourceIdentityDetail>> saveArgs =
        ArgumentCaptor.forClass(Iterable.class);

    ResponseEntity<List<Registration>> actual = controller.register(asList(id1, id2));

    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(actual.getBody())
        .containsExactlyInAnyOrder(registration("1", id1), registration("2", id2));
    verify(repo).saveAll(saveArgs.capture());
    assertThat(saveArgs.getValue()).containsExactlyInAnyOrder(newDetail("1", 1), newDetail("2", 2));
  }

  private ResourceIdentity resourceIdentity(int i) {
    return ResourceIdentity.builder().identifier("i" + i).resource("r" + i).system("s" + i).build();
  }
}
