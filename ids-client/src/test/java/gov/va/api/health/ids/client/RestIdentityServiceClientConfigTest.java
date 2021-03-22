package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncodedIdFormat.V2LookupHandler;
import gov.va.api.health.ids.client.EncodedIdFormat.V2RegistrationHandler;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook.Mapping;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import gov.va.api.health.ids.client.IdsClientProperties.EncodedIdsFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.PatientIcnFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.UuidFormatProperties;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientIcnLookupHandler;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientRegistrationHandler;
import gov.va.api.health.ids.client.UuidFormat.UuidLookupHandler;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class RestIdentityServiceClientConfigTest {

  @Mock RestTemplate rt;

  public static Stream<Arguments> createWithFormats() {
    // boolean patientEnabled, boolean i3Enabled, boolean i2Enabled, boolean uuidEnabled
    final boolean o = true;
    final boolean x = false;
    return Stream.of(
        arguments(o, o, o, o),
        arguments(o, o, o, x),
        arguments(o, o, x, o),
        arguments(o, o, x, x),
        arguments(o, x, o, o),
        arguments(o, x, o, x),
        arguments(o, x, x, o),
        arguments(o, x, x, x),
        arguments(x, o, o, o),
        arguments(x, o, o, x),
        arguments(x, o, x, o),
        arguments(x, o, x, x),
        arguments(x, x, o, o),
        arguments(x, x, o, x),
        arguments(x, x, x, o)
        // disallowed: (x, x, x, x)
        //
        );
  }

  private void assertFormatType(
      Format format,
      Class<? extends LookupHandler> lookupType,
      Class<? extends RegistrationHandler> registrationType) {
    assertThat(format.lookupHandler()).isInstanceOf(lookupType);
    if (registrationType == null) {
      assertThat(format.registrationHandler()).isEmpty();
    } else {
      assertThat(format.registrationHandler().get()).isInstanceOf(registrationType);
    }
  }

  @ParameterizedTest
  @MethodSource
  void createWithFormats(
      boolean patientEnabled, boolean i3Enabled, boolean i2Enabled, boolean uuidEnabled) {
    IdsClientProperties properties =
        IdsClientProperties.builder()
            .patientIcn(
                PatientIcnFormatProperties.builder()
                    .idPattern("[0-9]+")
                    .enabled(patientEnabled)
                    .build())
            .encodedIds(
                EncodedIdsFormatProperties.builder()
                    .encodingKey("whatever")
                    .i3Enabled(i3Enabled)
                    .i2Enabled(i2Enabled)
                    .build())
            .uuid(
                UuidFormatProperties.builder()
                    .url("http://whatever.com")
                    .enabled(uuidEnabled)
                    .build())
            .build();
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    Codebook codebook = Codebook.builder().map(List.of(Mapping.of("WHATEVER", "W"))).build();
    EncodingIdentityServiceClient c =
        (EncodingIdentityServiceClient)
            new RestIdentityServiceClientConfig(rt, properties, null)
                .encodingIdentityServiceClient(codebook);
    /* order is [patient icn, i3, i2, uuid] formats, but any given format can be missing. */
    var formats = c.formats().iterator();
    if (properties.getPatientIcn().isEnabled()) {
      assertFormatType(
          formats.next(), PatientIcnLookupHandler.class, PatientRegistrationHandler.class);
    }
    if (properties.getEncodedIds().isEnabled()) {
      if (properties.getEncodedIds().isI3Enabled()) {
        assertFormatType(formats.next(), V2LookupHandler.class, V2RegistrationHandler.class);
      }
      if (properties.getEncodedIds().isI2Enabled()) {
        assertFormatType(formats.next(), V2LookupHandler.class, V2RegistrationHandler.class);
      }
    }
    if (properties.getUuid().isEnabled()) {
      assertFormatType(formats.next(), UuidLookupHandler.class, null);
    }
  }

  @SuppressWarnings("deprecation")
  @Test
  public void deprecatedConstructorIsStillSupported() {
    IdentityService ids =
        new RestIdentityServiceClientConfig(
                rt, new RestIdentityServiceClientProperties(null, "secret", ".*"))
            .encodingIdentityServiceClient(Codebook.builder().build());
    assertThat(ids).isInstanceOf(EncodingIdentityServiceClient.class);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void deprecatedRestIdentityServiceClientIsStillSupported() {
    IdentityService ids =
        new RestIdentityServiceClientConfig(
                rt, new RestIdentityServiceClientProperties("http://whatever.com", "secret", ".*"))
            .restIdentityServiceClient();
    assertThat(ids).isInstanceOf(RestIdentityServiceClient.class);
  }

  @Test
  public void encodingIdentityServiceClientThrowsExceptionIfNotConfigured() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                new RestIdentityServiceClientConfig(
                        rt,
                        IdsClientProperties.builder().build(),
                        new RestIdentityServiceClientProperties(null, null, ".*"))
                    .encodingIdentityServiceClient(Codebook.builder().build()));
  }
}
