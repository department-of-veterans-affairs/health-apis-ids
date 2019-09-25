package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.DelimitedRepresentation;
import gov.va.api.health.ids.client.EncryptingIdEncoder.IncompleteResourceIdentity;
import gov.va.api.health.ids.client.EncryptingIdEncoder.UnknownRepresentation;
import org.junit.Test;

public class EncryptingIdEncoderTest {

  @Test
  public void delimitedRepresentationTo() {
    assertThat(
            DelimitedRepresentation.to(
                ResourceIdentity.builder()
                    .system("WHATEVER")
                    .resource("ANYTHING")
                    .identifier("ABC:123")
                    .build()))
        .isEqualTo("WHATEVER:ANYTHING:ABC:123");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingId1() {
    DelimitedRepresentation.from("WHATEVER:ANYTHING");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingId2() {
    DelimitedRepresentation.from("WHATEVER:ANYTHING:");
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingIdentity() {
    DelimitedRepresentation.to(
        ResourceIdentity.builder().system("WHATEVER").resource("ANYTHING").build());
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource() {
    DelimitedRepresentation.to(
        ResourceIdentity.builder().system("WHATEVER").identifier("ABC:123").build());
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource1() {
    DelimitedRepresentation.from("WHATEVER");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingResource2() {
    DelimitedRepresentation.from("WHATEVER:");
  }

  @Test(expected = IncompleteResourceIdentity.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingSystem() {
    DelimitedRepresentation.to(
        ResourceIdentity.builder().resource("ANYTHING").identifier("ABC:123").build());
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingValues1() {
    DelimitedRepresentation.from("::");
  }

  @Test(expected = UnknownRepresentation.class)
  public void delimitedRepresentationToThrowsExceptionIfMissingValues2() {
    DelimitedRepresentation.from(" : : ");
  }

  @Test
  public void encodingIsReproducible() {
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded1 = EncryptingIdEncoder.builder().password("magic-ids").build().encode(original);
    String encoded2 = EncryptingIdEncoder.builder().password("magic-ids").build().encode(original);

    assertThat(encoded1).isEqualTo(encoded2);
  }

  @Test
  public void roundTrip() {
    EncryptingIdEncoder encoder = EncryptingIdEncoder.builder().password("magic-ids").build();
    ResourceIdentity original =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC:123")
            .build();
    String encoded = encoder.encode(original);
    System.out.println(encoded);
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
    String encoded = EncryptingIdEncoder.builder().password("magic-ids").build().encode(original);
    ResourceIdentity decoded =
        EncryptingIdEncoder.builder().password("magic-ids").build().decode(encoded);
    assertThat(decoded).isEqualTo(original);
  }
}
