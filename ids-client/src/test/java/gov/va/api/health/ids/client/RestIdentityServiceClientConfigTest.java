package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

public class RestIdentityServiceClientConfigTest {
  @Test
  public void encodingIdentityServiceClientUsesEncodingClientIfPasswordIsSet() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "secret", ".*")
            .encodingIdentityServiceClient(Codebook.builder().build());
    assertThat(ids).isInstanceOf(EncodingIdentityServiceClient.class);
  }

  @Test
  public void restIdentityServiceClientCanBeUsed() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "secret", ".*")
            .restIdentityServiceClient();
    assertThat(ids).isInstanceOf(RestIdentityServiceClient.class);
  }

  @Test
  public void restIdentityServiceClientUsesEncodingClientIfPasswordIsNotSet() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "", ".*")
            .encodingIdentityServiceClient(Codebook.builder().build());
    assertThat(ids).isInstanceOf(RestIdentityServiceClient.class);
    ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "disabled", ".*")
            .encodingIdentityServiceClient(Codebook.builder().build());
    assertThat(ids).isInstanceOf(RestIdentityServiceClient.class);
  }
}
