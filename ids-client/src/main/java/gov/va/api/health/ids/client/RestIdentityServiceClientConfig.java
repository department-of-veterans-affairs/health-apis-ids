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

  @Value("${identityservice.encodingKey}")
  private final String encodingKey;

  /**
   * Create a new IdentityService that uses encoded IDs and will fallback REST for communication for
   * legacy IDs.
   */
  @Bean
  public IdentityService encodingIdentityServiceClient() {
    return EncodingIdentityServiceClient.builder()
        .encoder(EncryptingIdEncoder.builder().password(encodingKey).build())
        .delegate(restIdentityServiceClient())
        .build();
  }

  /** Create a new IdentityService that uses REST for communication. */
  public IdentityService restIdentityServiceClient() {
    return RestIdentityServiceClient.builder()
        .baseRestTemplate(restTemplate)
        .newRestTemplateSupplier(RestTemplate::new)
        .url(url)
        .build();
  }
}
