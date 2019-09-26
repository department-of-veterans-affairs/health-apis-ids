package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.IdentityService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

public class RestIdentityServiceClientConfigTest {

  @Test
  public void identityServiceClientCanDisableLocalEncoding() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "disabled")
            .identityServiceClient();
    assertThat(ids).isInstanceOf(RestIdentityServiceClient.class);
  }

  @Test
  public void restIdentityServiceClientUsesEncodingClientIfPasswordIsSet() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com", "secret-password")
            .identityServiceClient();
    assertThat(ids).isInstanceOf(EncodingIdentityServiceClient.class);
  }
}
