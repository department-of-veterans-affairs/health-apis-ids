package gov.va.api.health.ids.client;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("identityservice")
@Data
@Accessors(fluent = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestIdentityServiceClientProperties {
  private String url;
  @Builder.Default private String encodingKey = "disabled";
  @Builder.Default private String patientIdPattern = "[0-9]{10}V[0-9]{6}";

  public boolean hasEncodingKey() {
    return isNotBlank(encodingKey) && !"disabled".equals(encodingKey);
  }

  public boolean hasUrl() {
    return isNotBlank(url);
  }
}
