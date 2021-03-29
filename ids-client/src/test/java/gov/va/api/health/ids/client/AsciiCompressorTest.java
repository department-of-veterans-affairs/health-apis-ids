package gov.va.api.health.ids.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsciiCompressorTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "@\001@\001", // creates a 0 second byte
        "N1011537977V693883+673+LCH;6929384.839997;14xxx",
        "0000000",
        "a",
        "ab",
        "abc",
        "abcd",
        "abcde",
        "abcdef",
        "abcdefg",
        "abcdefgh",
        "abcdefghi",
        "abcdefghij",
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
