package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.IdentityService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

public class RestIdentityServiceClientConfigTest {

  @Test
  public void restIdentityServiceClient() {
    RestTemplate rt = Mockito.mock(RestTemplate.class);
    IdentityService ids =
        new RestIdentityServiceClientConfig(rt, "http://example.com").restIdentityServiceClient();
    assertThat(ids).isNotNull();
  }
}
