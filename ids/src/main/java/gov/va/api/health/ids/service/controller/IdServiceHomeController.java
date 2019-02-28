package gov.va.api.health.ids.service.controller;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IdServiceHomeController {

  private static final YAMLMapper MAPPER = new YAMLMapper();

  @Value("classpath:/api-v1.yaml")
  private Resource openapi;

  /** The OpenAPI specific content in yaml form. */
  @SuppressWarnings("WeakerAccess")
  @Bean
  public String openapiContent() throws IOException {
    try (InputStream is = openapi.getInputStream()) {
      return StreamUtils.copyToString(is, Charset.defaultCharset());
    }
  }

  /**
   * Provide access to the OpenAPI as JSON via RESTful interface. This is also used as the /
   * redirect.
   */
  @GetMapping(
    value = {"/", "/openapi.json"},
    produces = "application/json"
  )
  @ResponseBody
  public Object openapiJson() throws IOException {
    return IdServiceHomeController.MAPPER.readValue(openapiContent(), Object.class);
  }

  /** Provide access to the OpenAPI yaml via RESTful interface. */
  @GetMapping(value = "/openapi.yaml", produces = "application/vnd.oai.openapi")
  @ResponseBody
  public String openapiYaml() throws IOException {
    return openapiContent();
  }
}
