package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.Format.LookupHandler;
import gov.va.api.health.ids.client.Format.RegistrationHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedIdFormatTest {

  @Mock IdEncoder encoder;

  @Test
  void lookupAcceptsI2Prefixes() {
    LookupHandler h = EncodedIdFormat.of(EncodedIdFormat.V2_PREFIX, encoder).lookupHandler();
    assertThat(h.accept("I2-ANYTHING")).isTrue();
    assertThat(h.accept("i2-ANYTHING")).isFalse();
    assertThat(h.accept("I2ANYTHING")).isFalse();
    assertThat(h.accept("I3-ANYTHING")).isFalse();
    assertThat(h.accept("ANYTHING")).isFalse();
  }

  @Test
  void lookupDecodesId() {
    ResourceIdentity id =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("123")
            .build();
    when(encoder.decode("ANY123")).thenReturn(id);
    LookupHandler h = EncodedIdFormat.of(EncodedIdFormat.V2_PREFIX, encoder).lookupHandler();
    assertThat(h.lookup("I2-ANY123")).containsExactly(id);
  }

  @Test
  void registerAcceptsAnything() {
    RegistrationHandler r =
        EncodedIdFormat.of(EncodedIdFormat.V2_PREFIX, encoder).registrationHandler().get();
    assertThat(
            r.accept(
                ResourceIdentity.builder()
                    .system("WHATEVER")
                    .resource("ANYTHING")
                    .identifier("123")
                    .build()))
        .isTrue();
  }

  @Test
  void registerRegistersAnything() {
    ResourceIdentity id =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("123")
            .build();
    when(encoder.encode(id)).thenReturn("ANY123");
    RegistrationHandler r =
        EncodedIdFormat.of(EncodedIdFormat.V2_PREFIX, encoder).registrationHandler().get();

    assertThat(r.register(id))
        .isEqualTo(
            Registration.builder().uuid("I2-ANY123").resourceIdentities(List.of(id)).build());
  }
}
