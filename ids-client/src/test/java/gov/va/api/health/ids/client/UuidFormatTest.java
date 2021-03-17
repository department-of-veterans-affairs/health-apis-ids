package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.ids.api.IdentityService;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.client.UuidFormat.UuidLookupHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UuidFormatTest {

  @Mock IdentityService delegate;

  @Test
  void lookupAcceptsUuids() {
    assertThat(UuidLookupHandler.of(delegate).accept(UUID.randomUUID().toString())).isTrue();
    assertThat(UuidLookupHandler.of(delegate).accept("not" + UUID.randomUUID().toString()))
        .isFalse();
  }

  @Test
  void lookupDelegatesToIdentityService() {
    var uuid = UUID.randomUUID().toString();
    ResourceIdentity id =
        ResourceIdentity.builder()
            .system("WHATEVER")
            .resource("ANYTHING")
            .identifier("ABC")
            .build();
    when(delegate.lookup(uuid)).thenReturn(List.of(id));
    assertThat(UuidLookupHandler.of(delegate).lookup(uuid)).containsExactly(id);
  }
}
