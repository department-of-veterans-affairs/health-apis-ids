package gov.va.api.health.ids.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.codec.binary.Base32;

/**
 * This ID encoder uses encryption as a means for grouping and obscuring data. It is not meant to
 * provide strong encryption or protect secrets. It's goal is encapsulate resource identity
 * information in a form that saves system, resource, and identity in a form that can be used in
 * URLS, etc.
 */
public class ObfuscatingIdEncoder implements IdEncoder {

  private final Codebook codebook;

  @Getter(AccessLevel.PACKAGE)
  private final DelimitedRepresentation delimitedRepresentation;

  @Builder
  @SneakyThrows
  ObfuscatingIdEncoder(@NonNull Codebook codebook) {
    this.codebook = codebook;
    delimitedRepresentation = new DelimitedRepresentation();
  }

  @Override
  @SneakyThrows
  public ResourceIdentity decode(String encoded) {
    byte[] encryptedBytes = UrlSafeEncoding.decode(encoded);
    String delimitedIdentity = new String(encryptedBytes, UTF_8);
    return delimitedRepresentation.from(delimitedIdentity);
  }

  @Override
  @SneakyThrows
  public String encode(ResourceIdentity resourceIdentity) {
    String delimitedIdentity = delimitedRepresentation.to(resourceIdentity);
    byte[] decryptedBytes = delimitedIdentity.getBytes(UTF_8);
    return UrlSafeEncoding.encode(decryptedBytes);
  }

  /**
   * Interface used to support tools. This allows other consumers to plugin their own Codebooks to
   * the ids-client-tools application.
   */
  public interface CodebookSupplier extends Supplier<Codebook> {}

  /**
   * This class provides ID shortening mappings. It can be used to perform _exact_ match shortening.
   * For example, "MEDICATION_STATEMENT" could be configured to be shortened to "S". Values that are
   * not registered as shortened, will pass through transparently.
   */
  public static class Codebook {

    private final Map<String, String> longToShort;

    private final Map<String, String> shortToLong;

    @Builder
    Codebook(@Singular("map") Collection<Mapping> map) {
      longToShort = new HashMap<>(map.size());
      shortToLong = new HashMap<>(map.size());
      for (var mapping : map) {
        if (longToShort.containsKey(mapping.longValue())) {
          throw new IllegalArgumentException("Duplicate long value: " + mapping.longValue());
        }
        longToShort.put(mapping.longValue(), mapping.shortValue());
        if (shortToLong.containsKey(mapping.shortValue())) {
          throw new IllegalArgumentException("Duplicate shortened value: " + mapping.shortValue());
        }
        shortToLong.put(mapping.shortValue(), mapping.longValue());
      }
    }

    String restore(String in) {
      return shortToLong.getOrDefault(in, in);
    }

    String shorten(String in) {
      return longToShort.getOrDefault(in, in);
    }

    @Value
    @AllArgsConstructor(staticName = "of")
    public static class Mapping {
      String longValue;
      String shortValue;
    }
  }

  /**
   * Thrown if a resource identity cannot be encoded because it is missing data, such as system or
   * resource.
   */
  static class IncompleteResourceIdentity extends IllegalArgumentException {

    IncompleteResourceIdentity(ResourceIdentity resourceIdentity) {
      super(resourceIdentity.toString());
    }
  }

  /**
   * Throw if a resource identity cannot be decoded. This does not mean it could not be decrypted,
   * but rather the decrypted value was missing information.
   */
  static class UnknownRepresentation extends IllegalArgumentException {

    UnknownRepresentation(String s) {
      super(s);
    }
  }

  /** This class encapsulates the encoding algorithm. */
  private static class UrlSafeEncoding {

    /**
     * We'll use Base 32 (https://www.ietf.org/rfc/rfc4648.txt) to encode. This uses characters A-Z
     * and numbers 2-7, making this URL safe and _reasonable_ nice looking. We'll also pad with 0
     * instead of the standard = character, to keep the ID looking nice, e.g.
     *
     * <pre>
     * I2-7VES7OYMFUBMJCV4OO5S6JNDSJMJL2IOZOJQK45OHMKPO2ZWWZUA0000
     * </pre>
     *
     * The downside is that these IDs are long, ~60 characters. However, the base 64 versions are
     * around ~48 characters and are much more _ugly_.
     */
    private static Base32 BASE = new Base32(false, (byte) '0');

    static byte[] decode(String encoded) {
      // return Base64.getUrlDecoder().decode(encoded.getBytes(UTF_8));
      return BASE.decode(encoded.getBytes(UTF_8));
    }

    static String encode(byte[] unencodedBytes) {
      return new String(BASE.encode(unencodedBytes), UTF_8);
      // return new String(Base64.getUrlEncoder().encode(unencodedBytes), UTF_8);
    }
  }

  /**
   * This encapsulates the logic for representing the resource identity as a simple string. These
   * are the unencrypted values of the ID.
   */
  class DelimitedRepresentation {

    private static final String DELIMITER = ":";

    ResourceIdentity from(String delimited) {
      int firstDelim = delimited.indexOf(DELIMITER);
      if (firstDelim == -1 || firstDelim >= delimited.length() - 2) {
        throw new UnknownRepresentation(delimited);
      }
      int secondDelim = delimited.indexOf(DELIMITER, firstDelim + 1);
      if (secondDelim == -1 || secondDelim >= delimited.length() - 1) {
        throw new UnknownRepresentation(delimited);
      }
      String system = delimited.substring(0, firstDelim);
      String resource = delimited.substring(firstDelim + 1, secondDelim);
      String identity = delimited.substring(secondDelim + 1);
      if (isBlank(system) || isBlank(resource) || isBlank(identity)) {
        throw new UnknownRepresentation(delimited);
      }
      return ResourceIdentity.builder()
          .system(codebook.restore(system))
          .resource(codebook.restore(resource))
          .identifier(identity)
          .build();
    }

    String to(ResourceIdentity resourceIdentity) {
      if (isBlank(resourceIdentity.system())
          || isBlank(resourceIdentity.resource())
          || isBlank(resourceIdentity.identifier())) {
        throw new IncompleteResourceIdentity(resourceIdentity);
      }
      return codebook.shorten(resourceIdentity.system())
          + DELIMITER
          + codebook.shorten(resourceIdentity.resource())
          + DELIMITER
          + resourceIdentity.identifier();
    }
  }
}
