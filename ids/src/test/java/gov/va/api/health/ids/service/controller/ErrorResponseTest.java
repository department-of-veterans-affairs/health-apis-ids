package gov.va.api.health.ids.service.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import lombok.SneakyThrows;
import org.junit.Test;

public class ErrorResponseTest {
  @Test
  public void errorResponse() {
    roundTrip(ErrorResponse.of(new RuntimeException("fugazi")));
  }

  @SneakyThrows
  private <T> void roundTrip(T object) {
    ObjectMapper mapper = new JacksonConfig().objectMapper();
    String json = mapper.writeValueAsString(object);
    Object evilTwin = mapper.readValue(json, object.getClass());
    assertThat(evilTwin).isEqualTo(object);
  }
}
