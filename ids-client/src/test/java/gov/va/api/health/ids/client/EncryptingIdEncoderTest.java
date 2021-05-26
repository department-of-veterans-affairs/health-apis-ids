package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook.Mapping;
import gov.va.api.health.ids.client.EncryptingIdEncoder.IncompleteResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.UnknownRepresentation;
import gov.va.api.health.ids.client.IdEncoder.BadId;
import java.util.List;
import org.junit.jupiter.api.Test;

public class EncryptingIdEncoderTest {
  @Test
  public void badIdIsThrownForGarbageIdBaseValue() {
    EncryptingIdEncoder encoder = encoder();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded = encoder.encode(original);
    assertThatExceptionOfType(BadId.class)
        .isThrownBy(() -> encoder.decode(encoded.replaceAll("[A-M]", "1")));
  }

  @Test
  public void badIdIsThrownForGarbageIdContent() {
    EncryptingIdEncoder encoder = encoder();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded = encoder.encode(original);
    assertThatExceptionOfType(BadId.class)
        .isThrownBy(() -> encoder.decode(encoded.replaceAll("[A-M]", "X")));
  }

  @Test
  public void badIdIsThrownForGarbageIdSize() {
    EncryptingIdEncoder encoder = encoder();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded = encoder.encode(original);
    assertThatExceptionOfType(BadId.class)
        .isThrownBy(() -> encoder.decode(encoded.substring(0, encoded.length() - 7)));
  }

  @Test
  public void codebookShortens() {
    Codebook cb =
        Codebook.builder()
            .map(List.of(Mapping.of("ONE", "1"), Mapping.of("TWO", "2"), Mapping.of("THREE", "3")))
            .build();
    assertThat(cb.shorten("ONE")).isEqualTo("1");
    assertThat(cb.restore("1")).isEqualTo("ONE");
    assertThat(cb.restore("11")).isEqualTo("11");
    assertThat(cb.shorten("11")).isEqualTo("11");
  }

  @Test
  public void codebookThrowsExceptionForDuplicateLongValues() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                Codebook.builder()
                    .map(
                        List.of(
                            Mapping.of("ONE", "O"),
                            Mapping.of("ONE", "T"),
                            Mapping.of("THREE", "T")))
                    .build());
  }

  @Test
  public void codebookThrowsExceptionForDuplicateShortValues() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                Codebook.builder()
                    .map(
                        List.of(
                            Mapping.of("ONE", "O"),
                            Mapping.of("TWO", "T"),
                            Mapping.of("THREE", "T")))
                    .build());
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

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingId1() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from("WHATEVER:ANYTHING"));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingId2() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from("WHATEVER:ANYTHING:"));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingIdentity() {
    assertThatExceptionOfType(IncompleteResourceIdentity.class)
        .isThrownBy(
            () ->
                encoder()
                    .delimitedRepresentation()
                    .to(
                        ResourceIdentity.builder()
                            .system("WHATEVER")
                            .resource("ANYTHING")
                            .build()));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingResource() {
    assertThatExceptionOfType(IncompleteResourceIdentity.class)
        .isThrownBy(
            () ->
                encoder()
                    .delimitedRepresentation()
                    .to(
                        ResourceIdentity.builder()
                            .system("WHATEVER")
                            .identifier("ABC:123")
                            .build()));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingResource1() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from("WHATEVER"));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingResource2() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from("WHATEVER:"));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingSystem() {
    assertThatExceptionOfType(IncompleteResourceIdentity.class)
        .isThrownBy(
            () ->
                encoder()
                    .delimitedRepresentation()
                    .to(
                        ResourceIdentity.builder()
                            .resource("ANYTHING")
                            .identifier("ABC:123")
                            .build()));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingValues1() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from("::"));
  }

  @Test
  public void delimitedRepresentationToThrowsExceptionIfMissingValues2() {
    assertThatExceptionOfType(UnknownRepresentation.class)
        .isThrownBy(() -> encoder().delimitedRepresentation().from(" : : "));
  }

  public EncryptingIdEncoder encoder() {
    Codebook codebook =
        Codebook.builder()
            .map(
                List.of(
                    Mapping.of("WHATEVER", "W"),
                    Mapping.of("ANYTHING", "A"),
                    Mapping.of("CDW", "C"),
                    Mapping.of("MEDICATION_STATEMENT", "S")))
            .build();
    return EncryptingIdEncoder.builder().password("magic-ids").codebook(codebook).build();
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
    EncryptingIdEncoder encoder = encoder();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
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
