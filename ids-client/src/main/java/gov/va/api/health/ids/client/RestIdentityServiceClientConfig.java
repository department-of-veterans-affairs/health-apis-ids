package gov.va.api.health.ids.client;

import static gov.va.api.health.ids.client.EncryptingIdEncoder.BinaryRepresentations.compressedAscii;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.BinaryRepresentations.utf8;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.EncryptionMechanisms.aes;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.EncryptionMechanisms.blowfish;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.UrlSafeEncodings.base32;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.UrlSafeEncodings.base62;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

  private final IdsClientProperties properties;

  /** Constructor that includes the value annotations. */
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  public RestIdentityServiceClientConfig(
      @Autowired RestTemplate restTemplate,
      @Autowired(required = false) IdsClientProperties preferredProperties,
      @Autowired(required = false) RestIdentityServiceClientProperties deprecatedProperties) {
    this.restTemplate = restTemplate;
    properties = selectBestPropertiesOrDie(preferredProperties, deprecatedProperties);
  }

  @Deprecated
  public RestIdentityServiceClientConfig(
      RestTemplate restTemplate, RestIdentityServiceClientProperties deprecatedProperties) {
    this(restTemplate, null, deprecatedProperties);
  }

  /**
   * Create a new IdentityService that uses encoded IDs and will fallback REST for communication for
   * legacy IDs.
   */
  @SuppressWarnings({"WeakerAccess", "SpringJavaInjectionPointsAutowiringInspection"})
  @Bean
  @ConditionalOnMissingBean
  public IdentityService encodingIdentityServiceClient(
      @Autowired(required = false) Codebook maybeCodebook) {

    List<Format> formats = new ArrayList<>(4);
    if (properties.getPatientIcn().isEnabled()) {
      log.info("Supporting patient ICN matching {}", properties.getPatientIcn().getIdPattern());
      formats.add(PatientIcnFormat.of(properties.getPatientIcn().getIdPattern()));
    }

    if (properties.getEncodedIds().isEnabled()) {
      log.info("Using {} codebook", maybeCodebook == null ? "empty" : "provided");
      Codebook codebook = maybeCodebook == null ? Codebook.empty() : maybeCodebook;
      if (properties.getEncodedIds().isI3Enabled()) {
        log.info("Supporting I3 ids");
        formats.add(
            EncodedIdFormat.of(
                EncodedIdFormat.V3_PREFIX,
                EncryptingIdEncoder.builder()
                    .password(properties.getEncodedIds().getEncodingKey())
                    .codebook(codebook)
                    .textBinaryRepresentation(compressedAscii())
                    .encryptionMechanism(blowfish())
                    .encoding(base62())
                    .build()));
      }
      if (properties.getEncodedIds().isI2Enabled()) {
        log.info("Supporting I2 ids");
        formats.add(
            EncodedIdFormat.of(
                EncodedIdFormat.V2_PREFIX,
                EncryptingIdEncoder.builder()
                    .password(properties.getEncodedIds().getEncodingKey())
                    .codebook(codebook)
                    .textBinaryRepresentation(utf8())
                    .encryptionMechanism(aes())
                    .encoding(base32())
                    .build()));
      }
    }

    if (properties.getUuid().isEnabled()) {
      log.info("Support UUIDs from service at {}", properties.getUuid().getUrl());
      formats.add(
          UuidFormat.of(
              RestIdentityServiceClient.builder()
                  .baseRestTemplate(restTemplate)
                  .newRestTemplateSupplier(RestTemplate::new)
                  .url(properties.getUuid().getUrl())
                  .build()));
    }

    return EncodingIdentityServiceClient.of(formats);
  }

  /** Create a new IdentityService that uses REST for communication. */
  @SuppressWarnings("WeakerAccess")
  @Deprecated
  public IdentityService restIdentityServiceClient() {
    return RestIdentityServiceClient.builder()
        .baseRestTemplate(restTemplate)
        .newRestTemplateSupplier(RestTemplate::new)
        .url(properties.getUuid().getUrl())
        .build();
  }

  private IdsClientProperties selectBestPropertiesOrDie(
      IdsClientProperties preferredProperties,
      RestIdentityServiceClientProperties deprecatedProperties) {
    if (preferredProperties != null
        && preferredProperties.isValid()
        && preferredProperties.isEnabled()) {
      return preferredProperties;
    }
    if (deprecatedProperties != null && deprecatedProperties.isEnabled()) {
      log.warn("Use of identityservice properties is discouraged.");
      log.warn(
          "Update to preferred ids-client properties. See {}", IdsClientProperties.class.getName());
      return IdsClientProperties.from(deprecatedProperties);
    }

    throw new IllegalArgumentException(
        String.format("Missing configuration. See %s", IdsClientProperties.class.getName()));
  }
}
