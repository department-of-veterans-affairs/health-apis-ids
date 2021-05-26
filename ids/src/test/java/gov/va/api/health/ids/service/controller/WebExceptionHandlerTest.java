package gov.va.api.health.ids.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.ids.api.IdentityService.LookupFailed;
import gov.va.api.health.ids.api.IdentityService.RegistrationFailed;
import gov.va.api.health.ids.api.IdentityService.UnknownIdentity;
import gov.va.api.health.ids.service.controller.IdServiceV1ApiController.UuidGenerator;
import gov.va.api.health.ids.service.controller.impl.ResourceIdentityDetailRepository;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.stream.Stream;
import javax.validation.ConstraintViolationException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

public class WebExceptionHandlerTest {

  @Mock ResourceIdentityDetailRepository resources;

  @Mock UuidGenerator uuidGenerator;

  private IdServiceV1ApiController controller;
  private WebExceptionHandler exceptionHandler;

  static Stream<Arguments> parameters() {
    return Stream.of(
        arguments(HttpStatus.NOT_FOUND, new UnknownIdentity("1")),
        arguments(HttpStatus.BAD_REQUEST, new ConstraintViolationException(new HashSet<>())),
        arguments(HttpStatus.INTERNAL_SERVER_ERROR, new LookupFailed("1", "")),
        arguments(HttpStatus.INTERNAL_SERVER_ERROR, new RegistrationFailed("")),
        arguments(HttpStatus.INTERNAL_SERVER_ERROR, new RuntimeException()));
  }

  @BeforeEach
  public void _init() {
    MockitoAnnotations.initMocks(this);
    controller = new IdServiceV1ApiController(resources, uuidGenerator);
    exceptionHandler = new WebExceptionHandler();
  }

  private ExceptionHandlerExceptionResolver createExceptionResolver() {
    ExceptionHandlerExceptionResolver exceptionResolver =
        new ExceptionHandlerExceptionResolver() {
          @Override
          protected ServletInvocableHandlerMethod getExceptionHandlerMethod(
              HandlerMethod handlerMethod, Exception ex) {
            Method method =
                new ExceptionHandlerMethodResolver(WebExceptionHandler.class).resolveMethod(ex);
            assertThat(method).isNotNull();
            return new ServletInvocableHandlerMethod(exceptionHandler, method);
          }
        };
    exceptionResolver
        .getMessageConverters()
        .add(new MappingJackson2HttpMessageConverter(JacksonConfig.createMapper()));
    exceptionResolver.afterPropertiesSet();
    return exceptionResolver;
  }

  @ParameterizedTest
  @MethodSource(value = "parameters")
  @SneakyThrows
  public void expectStatus(HttpStatus status, Exception exception) {
    when(resources.findByUuid(Mockito.any())).thenThrow(exception);
    when(uuidGenerator.apply(Mockito.any())).thenReturn("x");
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setHandlerExceptionResolvers(createExceptionResolver())
            .build();

    mvc.perform(get("/api/v1/ids/123"))
        .andExpect(status().is(status.value()))
        .andExpect(jsonPath("type", equalTo(exception.getClass().getSimpleName())));
  }
}
