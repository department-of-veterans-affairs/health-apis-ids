package gov.va.api.health.ids.client;

import org.junit.Test;

public class ToolsTest {

  private String[] args(String... args) {
    return args;
  }

  @Test
  public void encodeAndDecode() {
    String previous = System.setProperty("password", "secret");
    try {
      Tools.main(args());
      Tools.main(args("CDW", "MEDICATION", "123456890"));
      Tools.main(args("I2-ZB3TEH5Q3BLZ4LVI7BI3T56FOPHDELLNGVS5SAJOBX6FORFPIIKA0000"));
    } finally {
      if (previous == null) {
        System.clearProperty("password");
      } else {
        System.setProperty("password", previous);
      }
    }
  }

  @Test(expected = Tools.MissingProperty.class)
  public void missingPropertyIsThrown() {
    Tools.main(args("CDW", "MEDICATION", "123456890"));
  }
}
