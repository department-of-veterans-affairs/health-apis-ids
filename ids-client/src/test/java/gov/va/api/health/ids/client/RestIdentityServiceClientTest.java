package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.IdentityService.LookupFailed;
import gov.va.api.health.ids.api.IdentityService.RegistrationFailed;
import gov.va.api.health.ids.api.IdentityService.UnknownIdentity;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.RestIdentityServiceClient.LookupErrorHandler;
import gov.va.api.health.ids.client.RestIdentityServiceClient.RegisterErrorHandler;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("unchecked")
public class RestIdentityServiceClientTest {
  @Rule public final ExpectedException thrown = ExpectedException.none();
  @Mock RestTemplate restTemplate;
  private RestIdentityServiceClient client;

  @Before
  public void _init() {
    MockitoAnnotations.initMocks(this);
    client = new RestIdentityServiceClient(restTemplate, "http://whatever.com");
  }

  @SneakyThrows
  private void assertLookupErrorHandler(
      Class<? extends Exception> exceptionType, HttpStatus status) {
    thrown.expect(exceptionType);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(status);
    LookupErrorHandler handler = new LookupErrorHandler("x");
    assertThat(handler.hasError(response)).isTrue();
    handler.handleError(response);
  }

  @SneakyThrows
  private void assertRegisterErrorHandler() {
    thrown.expect(RegistrationFailed.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    RegisterErrorHandler handler = new RegisterErrorHandler();
    assertThat(handler.hasError(response)).isTrue();
    handler.handleError(response);
  }

  private List<ResourceIdentity> identities() {
    ResourceIdentity a =
        ResourceIdentity.builder().identifier("a").system("CDW").resource("whatever").build();
    ResourceIdentity b = a.toBuilder().identifier("b").build();
    ResourceIdentity c = a.toBuilder().identifier("c").build();
    return Arrays.asList(a, b, c);
  }

  @Test
  public void lookupFailedExceptionIsThrownWhenBodyIsEmpty() {
    thrown.expect(LookupFailed.class);
    mockLookupResponse(HttpStatus.OK, Lists.emptyList());
    client.lookup("x");
  }

  @Test
  public void lookupFailedExceptionIsThrownWhenStatusIsAlsoNotOk() {
    assertLookupErrorHandler(LookupFailed.class, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  public void lookupFailedExceptionIsThrownWhenStatusIsNotOk() {
    assertLookupErrorHandler(LookupFailed.class, HttpStatus.BAD_REQUEST);
  }

  @Test
  public void lookupReturnsResourceIdentities() {
    List<ResourceIdentity> expected = identities();
    mockLookupResponse(HttpStatus.NOT_FOUND, expected);
    List<ResourceIdentity> actual = client.lookup("x");
    assertThat(actual).isEqualTo(expected);
    verify(restTemplate).setErrorHandler(Mockito.any(LookupErrorHandler.class));
  }

  private void mockLookupResponse(HttpStatus status, List<ResourceIdentity> body) {
    when(restTemplate.exchange(
            Mockito.anyString(),
            Mockito.eq(HttpMethod.GET),
            Mockito.any(HttpEntity.class),
            Mockito.any(ParameterizedTypeReference.class),
            Mockito.anyString()))
        .thenReturn(new ResponseEntity<>(body, status));
  }

  private void mockRegisterResponse(HttpStatus status, List<Registration> body) {
    when(restTemplate.exchange(
            Mockito.anyString(),
            Mockito.eq(HttpMethod.POST),
            Mockito.any(HttpEntity.class),
            Mockito.any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(body, status));
  }

  @Test
  public void registrationFailedExceptionIsThrownWhenBodyIsEmpty() {
    thrown.expect(RegistrationFailed.class);
    mockRegisterResponse(HttpStatus.OK, Lists.emptyList());
    client.register(identities());
  }

  @Test
  public void registrationFailedExceptionIsThrownWhenStatusIsNotOk() {
    assertRegisterErrorHandler();
  }

  @Test
  public void registrationReturnsResourceIdentities() {
    List<Registration> expected = registrations();
    mockRegisterResponse(HttpStatus.NOT_FOUND, expected);
    List<Registration> actual = client.register(identities());
    assertThat(actual).isEqualTo(expected);
    verify(restTemplate).setErrorHandler(Mockito.any(RegisterErrorHandler.class));
  }

  private List<Registration> registrations() {
    ResourceIdentity a =
        ResourceIdentity.builder().identifier("a").system("CDW").resource("whatever").build();
    ResourceIdentity b = a.toBuilder().identifier("b").build();
    ResourceIdentity c = a.toBuilder().identifier("c").build();
    Registration x = Registration.builder().resourceIdentity(a).uuid("A").build();
    Registration y = Registration.builder().resourceIdentity(b).uuid("B").build();
    Registration z = Registration.builder().resourceIdentity(c).uuid("C").build();
    return Arrays.asList(x, y, z);
  }

  @Test
  public void unknownIdentityExceptionIsThrownWhenStatusIs404() {
    assertLookupErrorHandler(UnknownIdentity.class, HttpStatus.NOT_FOUND);
  }
}
