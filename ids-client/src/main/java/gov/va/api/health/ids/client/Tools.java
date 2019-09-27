package gov.va.api.health.ids.client;

import static gov.va.api.health.ids.client.EncodingIdentityServiceClient.V2_PREFIX;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

@NoArgsConstructor(staticName = "tools")
public class Tools {

  private static String appName() {
    return System.getProperty("app.name", "ids-client-tools");
  }

  public static void main(String[] args) {
    switch (args.length) {
      case 1:
        tools().decode(args[0]);
        break;
      case 3:
        tools().encode(args[0], args[1], args[2]);
        break;
      default:
        usage();
    }
  }

  private static void usage() {
    System.out.println(
        List.of(
                "Usage:",
                appName() + " <encoded-id>",
                appName() + " <system> <resource> id",
                "",
                "System properties:",
                "-Dpassword=<password>")
            .stream()
            .collect(Collectors.joining("\n")));
  }

  private void decode(String id) {
    ResourceIdentity identity = encoder().decode(id.replace(V2_PREFIX, ""));
    System.out.println(identity.system() + " " + identity.resource() + " " + identity.identifier());
  }

  private void encode(String system, String resource, String identifier) {
    System.out.println(
        V2_PREFIX
            + encoder()
                .encode(
                    ResourceIdentity.builder()
                        .system(system)
                        .resource(resource)
                        .identifier(identifier)
                        .build()));
  }

  private EncryptingIdEncoder encoder() {
    return EncryptingIdEncoder.builder().password(password()).build();
  }

  private String password() {
    return property("password");
  }

  private String property(String name) {
    String value = System.getProperty(name);
    if (isBlank(value)) {
      throw new MissingProperty(name);
    }
    return value;
  }

  private static final class MissingProperty extends RuntimeException {

    public MissingProperty(String property) {
      super(property + " (Specifiy with -D" + property + "=<value>)");
    }
  }
}
