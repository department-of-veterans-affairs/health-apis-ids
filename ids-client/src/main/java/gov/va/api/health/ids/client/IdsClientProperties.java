package gov.va.api.health.ids.client;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import javax.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@SuppressWarnings("DefaultAnnotationParam")
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("ids-client")
@Data
@Accessors(fluent = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
public class IdsClientProperties {

  private PatientIcnFormatProperties patientIcn;

  private EncodedIdsFormatProperties encodedIds;

  private UuidFormatProperties uuid;

  /** Lazy getter with default disabled configuration. */
  public EncodedIdsFormatProperties getEncodedIds() {
    if (encodedIds == null) {
      encodedIds = new EncodedIdsFormatProperties();
    }
    return encodedIds;
  }

  /** Lazy getter with default disabled configuration. */
  public PatientIcnFormatProperties getPatientIcn() {
    if (patientIcn == null) {
      patientIcn = new PatientIcnFormatProperties();
    }
    return patientIcn;
  }

  /** Lazy getter with default disabled configuration. */
  public UuidFormatProperties getUuid() {
    if (uuid == null) {
      uuid = new UuidFormatProperties();
    }
    return uuid;
  }

  public boolean isEnabled() {
    return getPatientIcn().isEnabled() || getEncodedIds().isEnabled() || getUuid().isEnabled();
  }

  @AssertTrue
  public boolean isValid() {
    return getPatientIcn().isValid() && getEncodedIds().isValid() && getUuid().isValid();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Validated
  public static class EncodedIdsFormatProperties {

    @ToString.Exclude private String encodingKey;
    @Builder.Default private boolean i2Enabled = false;
    @Builder.Default private boolean i3Enabled = false;

    public boolean isEnabled() {
      return isI2Enabled() || isI3Enabled();
    }

    @AssertTrue
    public boolean isValid() {
      return !isEnabled() || isNotBlank(getEncodingKey());
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Validated
  public static class PatientIcnFormatProperties {
    @Builder.Default private boolean enabled = false;
    @Builder.Default private String idPattern = "[0-9]{10}V[0-9]{6}";

    @AssertTrue
    public boolean isValid() {
      return !isEnabled() || isNotBlank(getIdPattern());
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Validated
  public static class UuidFormatProperties {
    @Builder.Default private boolean enabled = false;
    private String url;

    @AssertTrue
    public boolean isValid() {
      return !isEnabled() || isNotBlank(getUrl());
    }
  }
}
