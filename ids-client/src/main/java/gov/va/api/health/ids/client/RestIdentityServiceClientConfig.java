package gov.va.api.health.ids.client;

import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
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
@Slf4j
public class RestIdentityServiceClientConfig {
  private final RestTemplate restTemplate;

  private final String url;

  private final String encodingKey;

  private final String patientIdPattern;

  /**
   * Constructor that includes the value annotations.
   *
   * @param url The identity service url
   * @param encodingKey The encoding key to use
   * @param patientIdPattern The patient id pattern
   * @param restTemplate The rest template
   */
  public RestIdentityServiceClientConfig(
      @Autowired RestTemplate restTemplate,
      @Value("${identityservice.url}") String url,
      @Value("${identityservice.encodingKey:disabled}") String encodingKey,
      @Value("${identityservice.patientIdPattern:[0-9]{10}V[0-9]{6}}") String patientIdPattern) {
    this.url = url;
    this.encodingKey = encodingKey;
    this.patientIdPattern = patientIdPattern;
    this.restTemplate = restTemplate;
  }

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
      log.info("Encoding Identity Service has been disabled.");
      return createRestIdentityServiceClient();
    }
    log.info("Using encoding Identity Service with patient ID pattern '{}'", patientIdPattern);
    return EncodingIdentityServiceClient.builder()
        .encoder(EncryptingIdEncoder.builder().password(encodingKey).codebook(codebook).build())
        .delegate(createRestIdentityServiceClient())
        .patientIdPattern(patientIdPattern)
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
