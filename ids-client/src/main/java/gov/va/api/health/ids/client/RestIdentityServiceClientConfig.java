package gov.va.api.health.ids.client;

import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

  @Value("${identityservice.url:disabled}")
  private final String encodingKey;

  private RestIdentityServiceClient createRestIdentityServiceClient() {
    /*
     * This is extracted with out the Spring annotation so it can be re-used without causing an
     * 'unsatisfied bean exception'.
     */
    return RestIdentityServiceClient.builder()
        .baseRestTemplate(restTemplate)
        .newRestTemplateSupplier(RestTemplate::new)
        .url(url)
        .build();
  }

  /**
   * Create a new IdentityService that uses encoded IDs and will fallback REST for communication for
   * legacy IDs.
   */
  @SuppressWarnings("WeakerAccess")
  @Bean
  @ConditionalOnBean(Codebook.class)
  public IdentityService encodingIdentityServiceClient(@Autowired Codebook codebook) {
    if (isBlank(encodingKey) || "disabled".equals(encodingKey)) {
      return createRestIdentityServiceClient();
    }
    log.info("Using encoding Identity Service");
    return EncodingIdentityServiceClient.builder()
        .encoder(EncryptingIdEncoder.builder().password(encodingKey).codebook(codebook).build())
        .delegate(createRestIdentityServiceClient())
        .build();
  }

  /** Create a new IdentityService that uses REST for communication. */
  @SuppressWarnings("WeakerAccess")
  @Bean
  @ConditionalOnMissingBean(Codebook.class)
  public IdentityService restIdentityServiceClient() {
    return createRestIdentityServiceClient();
  }
}
