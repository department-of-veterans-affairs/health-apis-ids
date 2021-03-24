package gov.va.api.health.ids.client;

import static org.apache.commons.lang3.StringUtils.isBlank;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import gov.va.api.health.ids.client.EncryptingIdEncoder.CodebookSupplier;
import gov.va.api.health.ids.client.IdsClientProperties.EncodedIdsFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.PatientIcnFormatProperties;
import gov.va.api.health.ids.client.IdsClientProperties.UuidFormatProperties;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(staticName = "tools")
public class Tools {
  private static String appName() {
    return System.getProperty("app.name", "ids-client-tools");
  }

  /** Tool main called from command line. */
  public static void main(String[] args) {
    try {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.OFF);
    } catch (Exception e) {
      // ignored... we tried to disabled logging.
    }
    if (args.length == 1) {
      tools().decode(args[0]);
    } else if (args.length == 3) {
      tools().encode(args[0], args[1], args[2]);
    } else {
      usage();
    }
  }

  private static void usage() {
    System.out.println(
        String.join(
            "\n",
            List.of(
                "Usage:",
                appName() + " <encoded-id>",
                appName() + " <system> <resource> id",
                "System properties:",
                "-Dpassword=<password>")));
  }

  private void decode(String id) {
    List<ResourceIdentity> identities = encoder().lookup(id);
    if (identities.isEmpty()) {
      throw new IllegalArgumentException("Cannot determine identity of " + id);
    }
    ResourceIdentity identity = identities.get(0);
    System.out.println(identity.system() + " " + identity.resource() + " " + identity.identifier());
  }

  private boolean enabled(String property) {
    return BooleanUtils.toBoolean(System.getProperty(property, "true"));
  }

  private void encode(String system, String resource, String identifier) {
    System.out.println(
        encoder()
            .register(
                List.of(
                    ResourceIdentity.builder()
                        .system(system)
                        .resource(resource)
                        .identifier(identifier)
                        .build()))
            .get(0)
            .uuid());
  }

  private IdentityService encoder() {
    var properties =
        IdsClientProperties.builder()
            .patientIcn(
                PatientIcnFormatProperties.builder()
                    .enabled(enabled("patient"))
                    .idPattern("[0-9]+(V[0-9]{6})?")
                    .build())
            .encodedIds(
                EncodedIdsFormatProperties.builder()
                    .i2Enabled(enabled("i2"))
                    .i3Enabled(enabled("i3"))
                    .encodingKey(password())
                    .build())
            .uuid(UuidFormatProperties.builder().enabled(false).build())
            .build();
    var config = new RestIdentityServiceClientConfig(null, properties, null);
    Optional<CodebookSupplier> codebooks = ServiceLoader.load(CodebookSupplier.class).findFirst();
    return config.encodingIdentityServiceClient(codebooks.orElse(Codebook::empty).get());
  }

  private String password() {
    return property("password");
  }

  private String property(@SuppressWarnings("SameParameterValue") String name) {
    String value = System.getProperty(name);
    if (isBlank(value)) {
      throw new MissingProperty(name);
    }
    return value;
  }

  static final class MissingProperty extends RuntimeException {
    MissingProperty(String property) {
      super(property + " (Specifiy with -D" + property + "=<value>)");
    }
  }
}
