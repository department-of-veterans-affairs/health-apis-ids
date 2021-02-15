package gov.va.api.health.ids.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.IdentitySubstitution.Operations;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IdentitySubstitutionTest {

  @Mock IdentityService ids;

  IdentitySubstitution<Ref> wp;

  @BeforeEach
  void _init() {
    wp =
        new IdentitySubstitution<>(
            ids,
            ref ->
                Optional.of(
                    ResourceIdentity.builder()
                        .system("IDS")
                        .resource(ref.type())
                        .identifier(ref.reference())
                        .build()),
            FugaziException::new);
  }

  @Test
  void privateIdOf() {
    when(ids.lookup(anyString())).thenReturn(List.of());
    assertThat(wp.privateIdOf("s", "nope")).isEmpty();

    ResourceIdentity sri1 =
        ResourceIdentity.builder().system("s").resource("r").identifier("1").build();
    ResourceIdentity xri1 =
        ResourceIdentity.builder().system("x").resource("r").identifier("2").build();
    when(ids.lookup(anyString())).thenReturn(List.of(xri1, sri1));
    assertThat(wp.privateIdOf("s", "p1")).isEqualTo(Optional.of("1"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void registerAndUpdateModifiesReferences() {
    when(ids.register(Mockito.any()))
        .thenReturn(
            List.of(
                registration("WITNESS", "wx"),
                registration("WITNESS", "wy"),
                registration("WITNESS", "wz"),
                registration("WHATEVER", "x"),
                registration("WHATEVER", "y"),
                registration("WHATEVER", "z"),
                registration("EVERYONE", "a")));
    Witness x = Witness.of("wxcdw");
    Witness y = Witness.of("wycdw");
    Witness z = Witness.of("wzcdw");
    Map<String, List<Ref>> refs =
        Map.of(
            x.originalId(),
            List.of(
                Ref.of().type("whatever").reference("xcdw").build(),
                Ref.of().type("everyone").reference("acdw").build()),
            y.originalId(),
            List.of(
                Ref.of().type("whatever").reference("ycdw").build(),
                Ref.of().type("everyone").reference("acdw").build()),
            z.originalId(),
            List.of(
                Ref.of().type("whatever").reference("zcdw").build(),
                Ref.of().type("everyone").reference("acdw").build()));

    Operations<Witness, Ref> operations =
        Operations.<Witness, Ref>builder()
            .toReferences(w -> refs.get(w.cdwId()).stream())
            .isReplaceable(r -> true)
            .resourceNameOf(reference -> reference.type().toUpperCase(Locale.ENGLISH))
            .privateIdOf(Ref::reference)
            .updatePrivateIdToPublicId((ref, id) -> ref.reference(id.orElse("")))
            .build();

    var idmap = wp.register(List.of(x, y, z), operations.toReferences());
    idmap.replacePrivateIdsWithPublicIds(List.of(x, y, z), operations);

    assertThat(refs.get(x.originalId()).get(0).reference()).isEqualTo("x");
    assertThat(refs.get(x.originalId()).get(1).reference()).isEqualTo("a");
    assertThat(refs.get(y.originalId()).get(0).reference()).isEqualTo("y");
    assertThat(refs.get(y.originalId()).get(1).reference()).isEqualTo("a");
    assertThat(refs.get(z.originalId()).get(0).reference()).isEqualTo("z");
    assertThat(refs.get(z.originalId()).get(1).reference()).isEqualTo("a");
  }

  @Test
  public void registerEmptyReturnsEmpty() {
    assertThat(wp.register(null)).isEmpty();
    assertThat(wp.register(List.of())).isEmpty();
  }

  private Registration registration(String resource, String id) {
    return Registration.builder()
        .uuid(id)
        .resourceIdentities(
            List.of(
                ResourceIdentity.builder()
                    .system("CDW")
                    .resource(resource)
                    .identifier(id + "cdw")
                    .build()))
        .build();
  }

  @Test
  public void toResourceIdentityExceptionTest() {
    when(ids.lookup(anyString())).thenReturn(List.of());
    assertThrows(FugaziException.class, () -> wp.toResourceIdentity("not cool"));
  }

  @Test
  public void toResourceIdentityTest() {
    ResourceIdentity coolResource =
        ResourceIdentity.builder().system("CDW").resource("COMMUNITY").identifier("ABED").build();
    when(ids.lookup("cool")).thenReturn(List.of(coolResource));
    assertThat(wp.toResourceIdentity("cool")).isEqualTo(coolResource);
  }

  public static class FugaziException extends RuntimeException {
    public FugaziException(String message) {
      super(message);
    }
  }

  @Data
  @Builder(builderMethodName = "of")
  private static class Ref {
    String system;
    String type;
    String reference;
  }

  @Data
  @AllArgsConstructor
  private static class Witness {
    private String objectType;

    private String cdwId;

    private String originalId;

    static Witness of(String id) {
      return new Witness("Witness", id, id);
    }
  }
}
