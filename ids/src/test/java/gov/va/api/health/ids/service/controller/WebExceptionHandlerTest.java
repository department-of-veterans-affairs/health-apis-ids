package gov.va.api.health.ids.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.validation.ConstraintViolationException;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
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

@SuppressWarnings("DefaultAnnotationParam")
@RunWith(Parameterized.class)
public class WebExceptionHandlerTest {
  @Parameter(0)
  public HttpStatus status;

  @Parameter(1)
  public Exception exception;

  @Mock ResourceIdentityDetailRepository resources;

  @Mock UuidGenerator uuidGenerator;

  private IdServiceV1ApiController controller;
  private WebExceptionHandler exceptionHandler;

  @Parameterized.Parameters(name = "{index}:{0} - {1}")
  public static List<Object[]> parameters() {
    return Arrays.asList(
        test(HttpStatus.NOT_FOUND, new UnknownIdentity("1")),
        test(HttpStatus.BAD_REQUEST, new ConstraintViolationException(new HashSet<>())),
        test(HttpStatus.INTERNAL_SERVER_ERROR, new LookupFailed("1", "")),
        test(HttpStatus.INTERNAL_SERVER_ERROR, new RegistrationFailed("")),
        test(HttpStatus.INTERNAL_SERVER_ERROR, new RuntimeException())
        //
        );
  }

  private static Object[] test(HttpStatus status, Exception exception) {
    return new Object[] {status, exception};
  }

  @Before
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

  @Test
  @SneakyThrows
  public void expectStatus() {
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
