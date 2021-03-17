package gov.va.api.health.ids.client;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@Slf4j
public class RestIdentityServiceClientConfig {
  private final RestTemplate restTemplate;

  private RestIdentityServiceClientProperties properties;

  /** Constructor that includes the value annotations. */
  public RestIdentityServiceClientConfig(
      @Autowired RestTemplate restTemplate,
      @Autowired RestIdentityServiceClientProperties properties) {
    this.properties = properties;
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
        .url(properties.getUrl())
        .build();
  }

  /**
   * Create a new IdentityService that uses encoded IDs and will fallback REST for communication for
   * legacy IDs.
   */
  @SuppressWarnings("WeakerAccess")
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(Codebook.class)
  public IdentityService encodingIdentityServiceClient(@Autowired Codebook codebook) {
    boolean useEncoder = properties.hasEncodingKey();
    boolean useService = properties.hasUrl();
    if (useEncoder && useService) {
      log.info(
          "Using encoding Identity Service with patient ID pattern '{}'"
              + " and rest Identity Service fallback",
          properties.getPatientIdPattern());
      return EncodingIdentityServiceClient.builder()
          .encoder(
              EncryptingIdEncoder.builder()
                  .password(properties.getEncodingKey())
                  .codebook(codebook)
                  .build())
          .delegate(createRestIdentityServiceClient())
          .patientIdPattern(properties.getPatientIdPattern())
          .build();
    }
    if (useEncoder) {
      log.info(
          "Using encoding Identity Service with patient ID pattern '{}'",
          properties.getPatientIdPattern());
      return EncodingIdentityServiceClient.builder()
          .encoder(
              EncryptingIdEncoder.builder()
                  .password(properties.getEncodingKey())
                  .codebook(codebook)
                  .build())
          .patientIdPattern(properties.getPatientIdPattern())
          .build();
    }
    if (useService) {
      log.info("Using rest Identity Service Client.");
      return createRestIdentityServiceClient();
    }
    throw new IllegalStateException("Identity service is not configured.");
  }

  /** Create a new IdentityService that uses REST for communication. */
  @SuppressWarnings("WeakerAccess")
  @Bean
  @ConditionalOnMissingBean({IdentityService.class, Codebook.class})
  public IdentityService restIdentityServiceClient() {
    return createRestIdentityServiceClient();
  }
}
