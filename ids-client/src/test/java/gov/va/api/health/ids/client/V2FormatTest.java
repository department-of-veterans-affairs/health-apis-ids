package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.V2Format.V2LookupHandler;
import gov.va.api.health.ids.client.V2Format.V2RegistrationHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V2FormatTest {

  @Mock IdEncoder encoder;

  @Test
  void lookupAcceptsI2Prefixes() {
    V2LookupHandler h = V2LookupHandler.of(encoder);
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
    V2LookupHandler h = V2LookupHandler.of(encoder);
    assertThat(h.lookup("I2-ANY123")).containsExactly(id);
  }

  @Test
  void registerAcceptsAnything() {
    V2RegistrationHandler r = V2RegistrationHandler.of(encoder);
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
    V2RegistrationHandler r = V2RegistrationHandler.of(encoder);
    assertThat(r.register(id))
        .isEqualTo(
            Registration.builder().uuid("I2-ANY123").resourceIdentities(List.of(id)).build());
  }
}
