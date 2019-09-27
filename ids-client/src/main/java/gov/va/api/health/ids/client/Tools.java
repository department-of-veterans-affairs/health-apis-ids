package gov.va.api.health.ids.client;

import static gov.va.api.health.ids.client.EncodingIdentityServiceClient.V2_PREFIX;

import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.Codebook;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.CodebookSupplier;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

@NoArgsConstructor(staticName = "tools")
public class Tools {

  private static String appName() {
    return System.getProperty("app.name", "ids-client-tools");
  }

  /** Tool main called from command line. */
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
        List.of("Usage:", appName() + " <encoded-id>", appName() + " <system> <resource> id")
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

  private ObfuscatingIdEncoder encoder() {
    Optional<CodebookSupplier> codebooks = ServiceLoader.load(CodebookSupplier.class).findFirst();
    return ObfuscatingIdEncoder.builder()
        .codebook(codebooks.orElseGet(() -> new EmptyCodebookSupplier()).get())
        .build();
  }

  private static class EmptyCodebookSupplier implements CodebookSupplier {

    @Override
    public Codebook get() {
      return Codebook.builder().build();
    }
  }
}
