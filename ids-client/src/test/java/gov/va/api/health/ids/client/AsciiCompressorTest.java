package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsciiCompressorTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a",
        "abc",
        "abcdefgh",
        "abcdefghe",
        "abcdefghefgh",
        "01234567890123456789",
        "V:OB:N5000000347+673+CH;6919171.919997;14",
        "111 222 333 444 555 666 777",
        "~!@#$%^&*()_+{}|\\'\";:<>,./?`",
        "whitespaces \n\t\r\n\fwhitespaces"
      })
  void roundTrip(String s) {
    AsciiCompressor compressor = new AsciiCompressor();
    byte[] compressed = compressor.compress(s);
    assertThat(compressed.length).isLessThanOrEqualTo(s.length());
    String restored = compressor.decompress(compressed);
    assertThat(restored).isEqualTo(s);
  }
}
