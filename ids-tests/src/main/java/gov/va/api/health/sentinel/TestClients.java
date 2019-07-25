package gov.va.api.health.sentinel;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Utility class that provides a {@link TestClient} for interacting with the ID service, in local
 * environment only.
 */
@UtilityClass
public final class TestClients {
  static TestClient ids() {
    return BasicTestClient.builder()
        .service(idsServiceDefinition())
        .contentType("application/json")
        .mapper(JacksonConfig::createMapper)
        .build();
  }

  /** Return the applicable system definition for the current environment. */
  private static ServiceDefinition idsServiceDefinition() {
    switch (Environment.get()) {
      case LOCAL:
        return ServiceDefinition.builder()
            .url(SentinelProperties.optionUrl("ids", "http://localhost"))
            .port(8089)
            .accessToken(() -> Optional.empty())
            .apiPath(SentinelProperties.optionApiPath("ids", "/api/"))
            .build();

      case LAB:
        // falls through
      case PROD:
        // falls through
      case QA:
        // falls through
      case STAGING:
        throw new IllegalArgumentException("ID service is only accessible in LOCAL environment.");
      default:
        throw new IllegalArgumentException("Unknown sentinel environment: " + Environment.get());
    }
  }
}
