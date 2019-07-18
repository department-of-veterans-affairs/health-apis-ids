package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for identity service rest client.
 *
 * <p>Requires `identityservice.url` to be defined a property.
 */
@Configuration
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class RestIdentityServiceClientConfig {
  private final RestTemplate restTemplate;

  @Value("${identityservice.url}")
  private final String url;

  /** Create a new IdentityService that uses REST for communication. */
  @Bean
  public IdentityService restIdentityServiceClient() {
    return RestIdentityServiceClient.builder()
        .baseRestTemplate(restTemplate)
        .newRestTemplateSupplier(RestTemplate::new)
        .url(url)
        .build();
  }
}
