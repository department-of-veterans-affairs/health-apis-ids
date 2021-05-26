package gov.va.api.health.ids.service.controller.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.Test;

public class ResourceIdentityDetailTest {
  @Test
  void equals() {
    assertThat(ResourceIdentityDetail.builder().build().equals("NOPE")).isFalse();
    assertThat(
            ResourceIdentityDetail.builder()
                .build()
                .equals(ResourceIdentityDetail.builder().build()))
        .isTrue();
  }

  /** Verifies no arg constructor, setters, equality only compares ID field. */
  @Test
  public void jpaCompliance() {
    // has no arg constructor
    ResourceIdentityDetail detail = new ResourceIdentityDetail();
    detail.pk(1);
    detail.identifier("i1");
    detail.resource("r1");
    detail.system("s1");
    detail.stationIdentifier("st1");
    detail.uuid("youyuoueyedee");
    assertThat(detail.hashCode()).isEqualTo(Objects.hash(1));
    assertThat(detail).isEqualTo(ResourceIdentityDetail.builder().pk(1).build());
    assertThat(detail).isNotEqualTo(ResourceIdentityDetail.builder().pk(2).build());
    assertThat(detail).isNotEqualTo(null);
    assertThat(detail).isEqualTo(detail);
    assertThat(detail.toString()).isNotNull();
  }
}
