package gov.va.api.health.ids.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.codec.binary.Base32;

/**
 * This ID encoder uses encryption as a means for grouping and obscuring data. It is not meant to
 * provide strong encryption or protect secrets. It's goal is encapsulate resource identity
 * information in a form that saves system, resource, and identity in a form that can be used in
 * URLS, etc.
 */
public class EncryptingIdEncoder implements IdEncoder {
  private final SecretKeySpec secretKey;

  private final Codebook codebook;

  private final CipherPool ciphers = new CipherPool();

  @Getter(AccessLevel.PACKAGE)
  private final DelimitedRepresentation delimitedRepresentation = new DelimitedRepresentation();

  @Builder
  @SneakyThrows
  EncryptingIdEncoder(
      @NonNull String password,
      @SuppressWarnings("ParameterHidesMemberVariable") @NonNull Codebook codebook) {
    this.codebook = codebook;
    /* We'll need a key that we can use over and over. */
    KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt(), 10000, 128);
    secretKey =
        new SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(keySpec)
                .getEncoded(),
            "AES");
  }

  private static byte[] initializationVector() {
    /*
     * For strong encryption, you'd use some randomized iv. We don't need it since we're only
     * looking to slightly obscure system, resource, and id in such a way that we can decode it
     * later.
     */
    return Arrays.copyOf(EncryptingIdEncoder.class.getSimpleName().getBytes(UTF_8), 16);
  }

  private static byte[] salt() {
    /*
     * For strong encryption, you'd use randomized salt. But since we're not looking for strong
     * encryption, just obscured encoding. Furthermore, we need _repeatable_ ids and cannot use
     * randomization.
     */
    return Arrays.copyOf(EncryptingIdEncoder.class.getSimpleName().getBytes(UTF_8), 8);
  }

  @Override
  public ResourceIdentity decode(String encoded) {
    try {
      byte[] encryptedBytes = UrlSafeEncoding.decode(encoded);
      byte[] decryptedBytes = ciphers.decryptor().doFinal(encryptedBytes);
      String delimitedIdentity = new String(decryptedBytes, UTF_8);
      return delimitedRepresentation.from(delimitedIdentity);
    } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
      throw new BadId(encoded, e);
    }
  }

  @Override
  public String encode(ResourceIdentity resourceIdentity) {
    try {
      String delimitedIdentity = delimitedRepresentation.to(resourceIdentity);
      byte[] decryptedBytes = delimitedIdentity.getBytes(UTF_8);
      byte[] encryptedBytes = ciphers.encryptor().doFinal(decryptedBytes);
      return UrlSafeEncoding.encode(encryptedBytes);
    } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
      throw new EncodingFailed(resourceIdentity.toString(), e);
    }
  }

  /**
   * Interface used to support tools. This allows other consumers to plugin their own Codebooks to
   * the ids-client-tools application.
   */
  @SuppressWarnings("WeakerAccess")
  public interface CodebookSupplier extends Supplier<Codebook> {}

  /**
   * This class provides ID shortening mappings. It can be used to perform _exact_ match shortening.
   * For example, "MEDICATION_STATEMENT" could be configured to be shortened to "S". Values that are
   * not registered as shortened, will pass through transparently.
   */
  @SuppressWarnings("WeakerAccess")
  public static class Codebook {
    private final Map<String, String> longToShort;

    private final Map<String, String> shortToLong;

    @Builder
    Codebook(Collection<Mapping> map) {
      if (map == null) {
        longToShort = emptyMap();
        shortToLong = emptyMap();
        return;
      }
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

    public static Codebook empty() {
      return new Codebook(null);
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
    private static Base32 BASE = new Base32(1024, "".getBytes(UTF_8), false, (byte) '0');

    static byte[] decode(String encoded) {
      return BASE.decode(encoded.getBytes(UTF_8));
    }

    static String encode(byte[] unencodedBytes) {
      return new String(BASE.encode(unencodedBytes), UTF_8);
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

  /**
   * Ciphers are expensive to create and initialize. They are re-usable, but not thread safe. We'll
   * use a cypher-per-thread approach. We'll also need to keep encrypting and decrypting separate
   * because they are initialized differently.
   */
  @SuppressWarnings("ThreadLocalUsage")
  private class CipherPool {
    private final ThreadLocal<Cipher> encryptors = new ThreadLocal<>();

    private final ThreadLocal<Cipher> decryptors = new ThreadLocal<>();

    Cipher decryptor() {
      return get(decryptors, Cipher.DECRYPT_MODE);
    }

    Cipher encryptor() {
      return get(encryptors, Cipher.ENCRYPT_MODE);
    }

    @SneakyThrows
    private Cipher get(ThreadLocal<Cipher> pool, int mode) {
      Cipher cipher = pool.get();
      if (cipher == null) {
        cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(mode, secretKey, new IvParameterSpec(initializationVector()));
        pool.set(cipher);
      }
      return cipher;
    }
  }
}
