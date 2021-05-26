package gov.va.api.health.ids.service.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {IdServiceHomeController.class})
public class IdServiceHomeControllerTest {
  @Autowired private MockMvc mvc;

  @Test
  @SneakyThrows
  public void openapiJson() {
    mvc.perform(get("/openapi.json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi", equalTo("3.0.1")));
  }

  @Test
  @SneakyThrows
  public void openapiYaml() {
    String expected =
        StreamUtils.copyToString(
            getClass().getResourceAsStream("/api-v1.yaml"), StandardCharsets.UTF_8);
    mvc.perform(get("/openapi.yaml"))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo(expected)));
  }

  @Test
  @SneakyThrows
  public void openapiYamlFromIndex() {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi", equalTo("3.0.1")));
  }
}
