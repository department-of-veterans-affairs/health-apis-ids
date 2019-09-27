package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.Codebook;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.Codebook.Mapping;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.IncompleteResourceIdentity;
import gov.va.api.health.ids.client.ObfuscatingIdEncoder.UnknownRepresentation;
import org.junit.Test;

public class ObfuscatingIdEncoderTest {

  @Test
  public void codebookShortens() {
    Codebook cb =
        Codebook.builder()
            .map(Mapping.of("ONE", "1"))
            .map(Mapping.of("TWO", "2"))
            .map(Mapping.of("THREE", "3"))
            .build();
    assertThat(cb.shorten("ONE")).isEqualTo("1");
    assertThat(cb.restore("1")).isEqualTo("ONE");
    assertThat(cb.restore("11")).isEqualTo("11");
    assertThat(cb.shorten("11")).isEqualTo("11");
  }

  @Test(expected = IllegalArgumentException.class)
  public void codebookThrowsExceptionForDuplicateLongValues() {
    Codebook.builder()
        .map(Mapping.of("ONE", "O"))
        .map(Mapping.of("ONE", "T"))
        .map(Mapping.of("THREE", "T"))
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void codebookThrowsExceptionForDuplicateShortValues() {
    Codebook.builder()
        .map(Mapping.of("ONE", "O"))
        .map(Mapping.of("TWO", "T"))
        .map(Mapping.of("THREE", "T"))
        .build();
  }

  @Test
  public void delimitedRepresentationTo() {
    assertThat(
            encoder()
                .delimitedRepresentation()
                .to(
                    ResourceIdentity.builder()
                        .system("WHATEVER")
                        .resource("ANYTHING")
                        .identifier("ABC:123")
                        .build()))
        .isEqualTo("W:A:ABC:123");
    assertThat(
            encoder()
                .delimitedRepresentation()
                .to(
                    ResourceIdentity.builder()
                        .system("NOT_WHATEVER")
                        .resource("NOT_ANYTHING")
                        .identifier("ABC:123")
                        .build()))
        .isEqualTo("NOT_WHATEVER:NOT_ANYTHING:ABC:123");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingId1() {
    encoder().delimitedRepresentation().from("WHATEVER:ANYTHING");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingId2() {
    encoder().delimitedRepresentation().from("WHATEVER:ANYTHING:");
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingIdentity() {
    encoder()
        .delimitedRepresentation()
        .to(ResourceIdentity.builder().system("WHATEVER").resource("ANYTHING").build());
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource() {
    encoder()
        .delimitedRepresentation()
        .to(ResourceIdentity.builder().system("WHATEVER").identifier("ABC:123").build());
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource1() {
    encoder().delimitedRepresentation().from("WHATEVER");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource2() {
    encoder().delimitedRepresentation().from("WHATEVER:");
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingSystem() {
    encoder()
        .delimitedRepresentation()
        .to(ResourceIdentity.builder().resource("ANYTHING").identifier("ABC:123").build());
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingValues1() {
    encoder().delimitedRepresentation().from("::");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingValues2() {
    encoder().delimitedRepresentation().from(" : : ");
  }

  public ObfuscatingIdEncoder encoder() {
    Codebook codebook =
        Codebook.builder()
            .map(Mapping.of("WHATEVER", "W"))
            .map(Mapping.of("ANYTHING", "A"))
            .map(Mapping.of("CDW", "C"))
            .map(Mapping.of("MEDICATION_STATEMENT", "S"))
            .build();
    return ObfuscatingIdEncoder.builder().codebook(codebook).build();
  }

  @Test
  public void encodingIsReproducible() {
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded1 = encoder().encode(original);
    String encoded2 = encoder().encode(original);
    assertThat(encoded1).isEqualTo(encoded2);
  }

  @Test
  public void roundTrip() {
    ObfuscatingIdEncoder encoder = encoder();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("CDW")
            .resource("MEDICATION_STATEMENT")
            .identifier("ABCDEFGHI:1234567890")
            .build();
    String encoded = encoder.encode(original);
    ResourceIdentity decoded = encoder.decode(encoded);
    assertThat(decoded).isEqualTo(original);
  }

  @Test
  public void roundTripWithDifferentDecoder() {

    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded = encoder().encode(original);
    ResourceIdentity decoded = encoder().decode(encoded);
    assertThat(decoded).isEqualTo(original);
  }
}
