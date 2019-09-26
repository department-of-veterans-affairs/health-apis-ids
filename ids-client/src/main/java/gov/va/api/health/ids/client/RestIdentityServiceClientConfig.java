package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RestIdentityServiceClientConfig {
  private final RestTemplate restTemplate;

  @Value("${identityservice.url}")
  private final String url;

  @Value("${identityservice.encodingKey:disable}")
  private final String encodingKey;

  /**
   * Create a new IdentityService that uses encoded IDs and will fallback REST for communication for
   * legacy IDs.
   */
  public IdentityService encodingIdentityServiceClient() {
    return EncodingIdentityServiceClient.builder()
        .encoder(EncryptingIdEncoder.builder().password(encodingKey).build())
        .delegate(restIdentityServiceClient())
        .build();
  }

  /**
   * If the `encodingKey` has been set to `disabled` then ONLY the rest client will be used. Local
   * encoded IDs will be disabled.
   */
  @Bean
  public IdentityService identityServiceClient() {
    if (encodingKey.toLowerCase(Locale.ENGLISH).startsWith("disable")) {
      log.warn("Encoding Identity Service is disabled.");
      return restIdentityServiceClient();
    }
    return encodingIdentityServiceClient();
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
