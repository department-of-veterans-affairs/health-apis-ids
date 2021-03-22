package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gov.va.api.health.ids.client.IdsClientProperties.EncodedIdsFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.PatientIcnFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.UuidFormatProperties;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class IdsClientPropertiesTest {

  public static Stream<Arguments> fromRestIdentityServiceClientProperties() {
    return Stream.of(
        arguments(
            RestIdentityServiceClientProperties.builder()
                .encodingKey("secret")
                .patientIdPattern("[0-9]+")
                .url("http://old.com")
                .build()),
        arguments(
            RestIdentityServiceClientProperties.builder()
                .encodingKey(null)
                .patientIdPattern("[0-9]+")
                .url(null)
                .build()));
  }

  public static Stream<Arguments> validAndEnabledCombinations() {
    /*
     * boolean patientEnabled,
     * String pattern,
     * boolean i2Enabled,
     * boolean i3enabled,
     * String encodingKey,
     * boolean uuidEnabled,
     * String url,
     * boolean expectedValid,
     * boolean expectedEnabled
     */
    return Stream.of(
        // all on
        arguments(true, "[0-9]+", true, true, "secret", true, "http://uuid.com", true, true),
        // each one off
        arguments(false, null, true, true, "secret", true, "http://uuid.com", true, true),
        arguments(true, "[0-9]+", false, false, null, true, "http://uuid.com", true, true),
        arguments(true, "[0-9]+", true, true, "secret", false, null, true, true),
        // all off
        arguments(false, null, false, false, null, false, null, true, false),
        // each one on, but invalid
        arguments(true, null, true, true, "secret", true, "http://uuid.com", false, true),
        arguments(true, "[0-9]+", true, true, null, true, "http://uuid.com", false, true),
        arguments(true, "[0-9]+", true, true, "secret", true, null, false, true)

        //
        );
  }

  @Test
  void defaultInstancesIsValidAndNotEnabled() {
    var p = new EncodedIdsFormatProperties();
    assertThat(p.isValid()).as("valid").isTrue();
    assertThat(p.isEnabled()).as("enabled").isFalse();
  }

  @ParameterizedTest
  @MethodSource
  void fromRestIdentityServiceClientProperties(RestIdentityServiceClientProperties old) {
    IdsClientProperties p = IdsClientProperties.from(old);
    assertThat(p.getPatientIcn().isEnabled()).isTrue();
    assertThat(p.getPatientIcn().getIdPattern()).isEqualTo(old.getPatientIdPattern());

    assertThat(p.getEncodedIds().isI2Enabled()).isTrue();
    assertThat(p.getEncodedIds().isI3Enabled()).isFalse();
    assertThat(p.getEncodedIds().getEncodingKey()).isEqualTo(old.getEncodingKey());

    assertThat(p.getUuid().isEnabled()).isEqualTo(old.hasUrl());
    assertThat(p.getUuid().getUrl()).isEqualTo(old.getUrl());
  }

  @Test
  void toStringDoesNotLeakPassword() {
    var p =
        IdsClientProperties.builder()
            .patientIcn(
                PatientIcnFormatProperties.builder().enabled(true).idPattern("[0-9]+").build())
            .encodedIds(
                EncodedIdsFormatProperties.builder()
                    .encodingKey("SECRET")
                    .i2Enabled(true)
                    .i3Enabled(true)
                    .build())
            .uuid(UuidFormatProperties.builder().enabled(true).url("http://whatever.com").build())
            .build();
    assertThat(p.toString()).doesNotContain("SECRET");
  }

  @ParameterizedTest
  @MethodSource
  void validAndEnabledCombinations(
      boolean patientEnabled,
      String pattern,
      boolean i2Enabled,
      boolean i3enabled,
      String encodingKey,
      boolean uuidEnabled,
      String url,
      boolean expectedValid,
      boolean expectedEnabled) {
    var p =
        IdsClientProperties.builder()
            .patientIcn(
                PatientIcnFormatProperties.builder()
                    .enabled(patientEnabled)
                    .idPattern(pattern)
                    .build())
            .encodedIds(
                EncodedIdsFormatProperties.builder()
                    .encodingKey(encodingKey)
                    .i2Enabled(i2Enabled)
                    .i3Enabled(i3enabled)
                    .build())
            .uuid(UuidFormatProperties.builder().enabled(uuidEnabled).url(url).build())
            .build();
    assertThat(p.isValid()).as("valid").isEqualTo(expectedValid);
    assertThat(p.isEnabled()).as("enabled").isEqualTo(expectedEnabled);
  }
}
