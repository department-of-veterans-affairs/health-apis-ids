package gov.va.api.health.ids.service.controller.impl;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.UUID;
import lombok.Getter;
import org.junit.Test;

public class Type5UuidGeneratorTest {

  @Getter
  private final Type5UuidGenerator generator = new Type5UuidGenerator(UUID.randomUUID().toString());

  @Test
  public void cdwPatientResourcesUseProvidedIdentityInsteadOfGeneratingUUID() {
    ResourceIdentity id1 =
        ResourceIdentity.builder()
            .system("CDW")
            .resource("PATIENT")
            .identifier("Same Identifier")
            .build();
    ResourceIdentity id2 =
        ResourceIdentity.builder().system("NOT").resource("SPECIAL").identifier("i1").build();
    assertThat(generator.isSpecialPatient(id1)).isTrue();
    assertThat(generator.isSpecialPatient(id2)).isFalse();
    assertThat(generator.apply(id1)).isEqualToIgnoringCase("Same Identifier");
  }

  @Test
  public void generationIsDeterministic() {
    ResourceIdentity id =
        ResourceIdentity.builder().system("s1").resource("r1").identifier("i1").build();
    String uuid1 = generator().apply(id);
    String uuid2 = generator().apply(id);
    assertThat(uuid1).isNotBlank().isEqualTo(uuid2);
    // Throws exception if the uuid cannot be parsed as a proper UUID.
    assertThat(UUID.fromString(uuid1)).isNotNull();
  }

  @Test(expected = NullPointerException.class)
  public void nullValueIsRejected() {
    generator().apply(null);
  }
}
