package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/** Rest client for the identity service. */
@Slf4j
@Builder
public final class RestIdentityServiceClient implements IdentityService {

  /**
   * This rest template is used a base for each request. However, it is not used to make requests
   * directly.
   */
  @NonNull private final RestTemplate baseRestTemplate;

  @NonNull private final String url;

  /** This is used to create a new rest template that can be modified for each request. */
  @NonNull private final Supplier<RestTemplate> newRestTemplateSupplier;

  /**
   * If the given value is null, an IllegalStateException is thrown. Otherwise, the value is
   * returned. Use this method for clean inline null checks.
   */
  private static <T> T notNull(T maybe) {
    if (maybe == null) {
      throw new IllegalStateException("Expected non-null value");
    }
    return maybe;
  }

  private HttpHeaders headers() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return headers;
  }

  @Override
  public List<ResourceIdentity> lookup(String id) {
    log.info("Looking up {}", id);
    RestTemplate restTemplate = newRestTemplate(new LookupErrorHandler(id));
    ResponseEntity<List<ResourceIdentity>> response =
        notNull(
            restTemplate.exchange(
                url + "/api/resourceIdentity/{id}",
                HttpMethod.GET,
                new HttpEntity<List<ResourceIdentity>>(headers()),
                new ParameterizedTypeReference<List<ResourceIdentity>>() {},
                id));
    List<ResourceIdentity> body = notNull(response.getBody());
    log.info("{} {}", response.getStatusCode(), body);
    if (body.isEmpty()) {
      throw new LookupFailed(
          id, "No identities returned, but status was " + response.getStatusCode());
    }
    return body;
  }

  /**
   * RestTemplate shouldn't be modified since they can be re-used across threads. We will create a
   * new template per request using the standard template as a base. This method create a copy of
   * the base rest template with a custom error handler.
   */
  private RestTemplate newRestTemplate(ResponseErrorHandler errorHandler) {
    RestTemplate copy = newRestTemplateSupplier.get();
    if (baseRestTemplate.getMessageConverters() != null
        && !baseRestTemplate.getMessageConverters().isEmpty()) {
      copy.setMessageConverters(baseRestTemplate.getMessageConverters());
    }
    if (baseRestTemplate.getUriTemplateHandler() != null) {
      copy.setUriTemplateHandler(baseRestTemplate.getUriTemplateHandler());
    }
    if (baseRestTemplate.getInterceptors() != null
        && !baseRestTemplate.getInterceptors().isEmpty()) {
      copy.setInterceptors(baseRestTemplate.getInterceptors());
    }
    if (baseRestTemplate.getRequestFactory() != null) {
      copy.setRequestFactory(baseRestTemplate.getRequestFactory());
    }
    copy.setErrorHandler(errorHandler);
    return copy;
  }

  @Override
  public List<Registration> register(List<ResourceIdentity> identities) {
    log.info("Registering {} identities", identities.size());
    log.debug("Registering {}", identities);
    RestTemplate restTemplate = newRestTemplate(new RegisterErrorHandler());
    ResponseEntity<List<Registration>> response =
        notNull(
            restTemplate.exchange(
                url + "/api/resourceIdentity",
                HttpMethod.POST,
                new HttpEntity<>(identities, headers()),
                new ParameterizedTypeReference<List<Registration>>() {}));
    List<Registration> body = notNull(response.getBody());
    log.debug("{}: {} identities registered", response.getStatusCode(), body.size());
    if (body.isEmpty()) {
      throw new RegistrationFailed(
          "No registrations returned, but status was " + response.getStatusCode());
    }
    return body;
  }

  @AllArgsConstructor
  static class LookupErrorHandler implements ResponseErrorHandler {

    private final String id;

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw new UnknownIdentity(id);
      }
      if (response.getStatusCode() != HttpStatus.OK) {
        throw new LookupFailed(id, "Http Response: " + response.getStatusCode());
      }
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return response.getStatusCode().isError();
    }
  }

  static class RegisterErrorHandler implements ResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      if (response.getStatusCode() != HttpStatus.OK) {
        throw new RegistrationFailed("Http Response: " + response.getStatusCode());
      }
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return response.getStatusCode().isError();
    }
  }
}
