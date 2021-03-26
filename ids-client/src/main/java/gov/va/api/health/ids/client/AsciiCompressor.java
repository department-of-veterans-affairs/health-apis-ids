package gov.va.api.health.ids.client;

import java.util.BitSet;

/**
 * This class provides 8 to 7 bit compress for Strings that only contain ASCII characters.
 * String.getBytes(US_ASCII) returns a byte array that uses 1 bytes per character, with the high bit
 * always 0. This compressor only uses 7 bits, characters do not break evenly across bytes. For
 * example, the string "ABCDEFGH" will only require 7 bytes instead of the normal 8.
 */
public class AsciiCompressor {

  /**
   * Return a byte array where each character consumes 7 bits, crossing byte boundaries as needed.
   */
  public byte[] compress(String string) {
    BitSet sevenBitChars = new BitSet(string.length() * 7);
    int currentBit = 0;
    for (char character : string.toCharArray()) {
      /*
       * Let's look at every bit in the character to see if we need to set the corresponding bit in
       * our bit set, e.g. 's' is '01110011'.
       */
      for (int characterBit = 0; characterBit < 7; characterBit++) {
        int mask = (1 << characterBit);
        if ((character & mask) > 0) {
          sevenBitChars.set(currentBit);
        }
        currentBit++;
      }
    }
    return sevenBitChars.toByteArray();
  }

  /** Return a String constructed from the 7 bit ASCII byte array. */
  public String decompress(byte[] sevenBitChars) {
    BitSet bits = BitSet.valueOf(sevenBitChars);
    /*
     * We may not being using all of the bits. For example, for a "bs" we only used 14 bits, but we
     * will have had to allocated 16 bits. We'll need to ignore that remaining two bits in the last
     * byte.
     */
    StringBuilder decompressedMessage = new StringBuilder(sevenBitChars.length * 8 / 7 + 1);
    for (int currentBit = 0; currentBit < bits.size(); currentBit += 7) {
      byte[] bytes = bits.get(currentBit, currentBit + 7).toByteArray();
      if (bytes.length == 0) {
        break;
      }
      char character = (char) bytes[0];
      decompressedMessage.append(character);
    }
    return decompressedMessage.toString();
  }
}
