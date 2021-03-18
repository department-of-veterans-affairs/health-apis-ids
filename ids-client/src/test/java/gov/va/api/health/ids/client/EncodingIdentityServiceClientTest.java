package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import gov.va.api.health.ids.client.IdsClientProperties.EncodedIdsFormatProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

@Slf4j
class EncodingIdentityServiceClientTest {

  @Test
  void vistaFhirQueryLongIdUseCase() {
    // V:OB:N5000000347+673+CH;6919171.919997;14
    IdsClientProperties properties =
        IdsClientProperties.builder()
            .encodedIds(
                EncodedIdsFormatProperties.builder()
                    .encodingKey("whatever")
                    .i3Enabled(true)
                    .i2Enabled(false)
                    .build())
            .build();
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    Codebook codebook = Codebook.builder().map(List.of()).build();
    IdentityService i =
        new RestIdentityServiceClientConfig(rt, properties, null)
            .encodingIdentityServiceClient(codebook);

    /* The longest ID we've encounter that expands to >80 characters with I2. */
    List<Registration> registrations =
        i.register(
            List.of(
                ResourceIdentity.builder()
                    .system("V")
                    .resource("OB")
                    .identifier("N5000000347+673+CH;6919171.919997;14")
                    .build()));

    String uuid = registrations.get(0).uuid();
    assertThat(uuid.length()).isLessThan(65 - EncodedIdFormat.V3_PREFIX.length());
  }
}
