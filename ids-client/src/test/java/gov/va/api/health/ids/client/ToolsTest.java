package gov.va.api.health.ids.client;

import org.junit.Test;

public class ToolsTest {

  private String[] args(String... args) {
    return args;
  }

  @Test
  public void encodeAndDecode() {
    Tools.main(args());
    Tools.main(args("CDW", "MEDICATION", "123456890"));
    Tools.main(args("I2-INCFOOSNIVCESQ2BKREU6TR2GEZDGNBVGY4DSMA0"));
  }
}
