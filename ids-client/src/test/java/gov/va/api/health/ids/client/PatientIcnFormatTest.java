package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientIcnLookupHandler;
import gov.va.api.health.ids.client.PatientIcnFormat.PatientRegistrationHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PatientIcnFormatTest {

  @Test
  void lookupAcceptsMatchingPatterns() {
    var h = PatientIcnLookupHandler.of("[0-9]+");
    assertThat(h.accept("123")).isTrue();
    assertThat(h.accept("123a")).isFalse();
  }

  @Test
  void lookupReturnsMviIdentity() {
    var h = PatientIcnLookupHandler.of("[0-9]+");
    assertThat(h.lookup("123"))
        .containsExactly(
            ResourceIdentity.builder().system("MVI").resource("PATIENT").identifier("123").build());
  }

  @Test
  void register() {
    assertThat(
            new PatientRegistrationHandler()
                .register(
                    ResourceIdentity.builder()
                        .system("MVI")
                        .resource("pAtIeNt")
                        .identifier("123")
                        .build()))
        .isEqualTo(
            Registration.builder()
                .uuid("123")
                .resourceIdentities(
                    List.of(
                        ResourceIdentity.builder()
                            .system("MVI")
                            .resource("PATIENT")
                            .identifier("123")
                            .build()))
                .build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"PATIENT", "patient", "pAtIeNt"})
  void registerAcceptsPatientResources(String resource) {
    assertThat(
            new PatientRegistrationHandler()
                .accept(
                    ResourceIdentity.builder()
                        .system("MVI")
                        .resource(resource)
                        .identifier("123")
                        .build()))
        .isTrue();
    assertThat(
            new PatientRegistrationHandler()
                .accept(
                    ResourceIdentity.builder()
                        .system("MVI")
                        .resource("not" + resource)
                        .identifier("123")
                        .build()))
        .isFalse();
  }
}
