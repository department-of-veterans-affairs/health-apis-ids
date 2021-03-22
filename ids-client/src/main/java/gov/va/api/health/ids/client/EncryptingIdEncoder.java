package gov.va.api.health.ids.client;

import static gov.va.api.health.ids.client.EncryptingIdEncoder.BinaryRepresentations.utf8;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.EncryptionMechanisms.aes;
import static gov.va.api.health.ids.client.EncryptingIdEncoder.UrlSafeEncodings.base32;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.ResourceIdentity;
import io.seruco.encoding.base62.Base62;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import lombok.RequiredArgsConstructor;
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
  private final Codebook codebook;

  private final CipherPool ciphers;

  private final UrlSafeEncoding encoding;

  @Getter(AccessLevel.PACKAGE)
  private final DelimitedRepresentation delimitedRepresentation = new DelimitedRepresentation();

  private final BinaryRepresentation textBinaryRepresentation;

  @Builder
  @SneakyThrows
  EncryptingIdEncoder(
      @NonNull String password,
      @NonNull Codebook codebook,
      UrlSafeEncoding encoding,
      EncryptionMechanism encryptionMechanism,
      BinaryRepresentation textBinaryRepresentation) {
    this.codebook = codebook;
    /* To support backwards compatibility, assume defaults matching the early behavior. */
    this.encoding = encoding == null ? base32() : encoding;
    this.textBinaryRepresentation =
        textBinaryRepresentation == null ? utf8() : textBinaryRepresentation;
    final EncryptionMechanism usableEncryption =
        encryptionMechanism == null ? aes() : encryptionMechanism;
    ciphers =
        CipherPool.of(cipherMode -> usableEncryption.createInitializedCipher(password, cipherMode));
  }

  @Override
  public ResourceIdentity decode(String encoded) {
    try {
      byte[] encryptedBytes = encoding.decode(encoded);
      byte[] decryptedBytes = ciphers.decryptor().doFinal(encryptedBytes);
      String delimitedIdentity = textBinaryRepresentation.fromBytes(decryptedBytes);
      return delimitedRepresentation.from(delimitedIdentity);
    } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
      throw new BadId(encoded, e);
    }
  }

  @Override
  public String encode(ResourceIdentity resourceIdentity) {
    try {
      String delimitedIdentity = delimitedRepresentation.to(resourceIdentity);
      byte[] decryptedBytes = textBinaryRepresentation.asBytes(delimitedIdentity);
      byte[] encryptedBytes = ciphers.encryptor().doFinal(decryptedBytes);
      return encoding.encode(encryptedBytes);
    } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
      throw new EncodingFailed(resourceIdentity.toString(), e);
    }
  }

  public interface BinaryRepresentation {
    byte[] asBytes(String string);

    String fromBytes(byte[] bytes);
  }

  public interface EncryptionMechanism {
    @NonNull
    Cipher createInitializedCipher(String password, int cipherMode);
  }

  /**
   * Interface used to support tools. This allows other consumers to plugin their own Codebooks to
   * the ids-client-tools application.
   */
  @SuppressWarnings("WeakerAccess")
  public interface CodebookSupplier extends Supplier<Codebook> {}

  /** The encoding mechanism that provides a URL safe version of ID. */
  public interface UrlSafeEncoding {
    byte[] decode(String encoded);

    String encode(byte[] unencodedBytes);
  }

  public static class BinaryRepresentations {
    public static BinaryRepresentation compressedAscii() {
      return new CompressedAsciiBinaryRepresentation();
    }

    public static BinaryRepresentation utf8() {
      return new StandardUt8BinaryRepresentation();
    }

    private static class CompressedAsciiBinaryRepresentation implements BinaryRepresentation {
      private final AsciiCompressor compressor = new AsciiCompressor();

      @Override
      public byte[] asBytes(String string) {
        return compressor.compress(string);
      }

      @Override
      public String fromBytes(byte[] bytes) {
        return compressor.decompress(bytes);
      }
    }

    private static class StandardUt8BinaryRepresentation implements BinaryRepresentation {
      @Override
      public byte[] asBytes(String string) {
        return string.getBytes(UTF_8);
      }

      @Override
      public String fromBytes(byte[] bytes) {
        return new String(bytes, UTF_8);
      }
    }
  }

  /**
   * Ciphers are expensive to create and initialize. They are re-usable, but not thread safe. We'll
   * use a cypher-per-thread approach. We'll also need to keep encrypting and decrypting separate
   * because they are initialized differently.
   */
  @SuppressWarnings("ThreadLocalUsage")
  @RequiredArgsConstructor(staticName = "of")
  private static class CipherPool {
    private final ThreadLocal<Cipher> encryptors = new ThreadLocal<>();

    private final ThreadLocal<Cipher> decryptors = new ThreadLocal<>();

    private final Function<Integer, Cipher> newCipher;

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
        cipher = newCipher.apply(mode);
        pool.set(cipher);
      }
      return cipher;
    }
  }

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

  public static class EncryptionMechanisms {
    /** AES encryption. */
    public static EncryptionMechanism aes() {
      return SecretKeyCipher.builder()
          .passwordToSecretKeySpec(EncryptionMechanisms::aesSecretKeySpec)
          .secretKeyToCipher(EncryptionMechanisms::aesCipher)
          .build();
    }

    @SneakyThrows
    private static Cipher aesCipher(SecretKeySpec secretKeySpec, int cipherMode) {
      var cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(
          cipherMode,
          secretKeySpec,
          new IvParameterSpec(EncryptionMechanisms.initializationVector(16)));
      return cipher;
    }

    @SneakyThrows
    private static SecretKeySpec aesSecretKeySpec(String password) {
      return new SecretKeySpec(
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
              .generateSecret(
                  new PBEKeySpec(password.toCharArray(), EncryptionMechanisms.salt(), 10000, 128))
              .getEncoded(),
          "AES");
    }

    /** Blowfish encryption. */
    public static EncryptionMechanism blowfish() {
      return SecretKeyCipher.builder()
          .passwordToSecretKeySpec(EncryptionMechanisms::blowfishSecretKeySpec)
          .secretKeyToCipher(EncryptionMechanisms::blowfishCipher)
          .build();
    }

    @SneakyThrows
    private static Cipher blowfishCipher(SecretKeySpec secretKeySpec, int cipherMode) {
      var cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding");
      cipher.init(
          cipherMode,
          secretKeySpec,
          new IvParameterSpec(EncryptionMechanisms.initializationVector(8)));
      return cipher;
    }

    @SneakyThrows
    private static SecretKeySpec blowfishSecretKeySpec(String password) {
      return new SecretKeySpec(password.getBytes(UTF_8), "Blowfish");
    }

    private static byte[] initializationVector(int size) {
      /*
       * For strong encryption, you'd use some randomized iv. We don't need it since we're only
       * looking to slightly obscure system, resource, and id in such a way that we can decode it
       * later.
       */
      return Arrays.copyOf(EncryptingIdEncoder.class.getSimpleName().getBytes(UTF_8), size);
    }

    private static byte[] salt() {
      /*
       * For strong encryption, you'd use randomized salt. But since we're not looking for strong
       * encryption, just obscured encoding. Furthermore, we need _repeatable_ ids and cannot use
       * randomization.
       */
      return initializationVector(8);
    }

    private static class SecretKeyCipher implements EncryptionMechanism {
      private final Function<String, SecretKeySpec> passwordToSecretKeySpec;

      private final BiFunction<SecretKeySpec, Integer, Cipher> secretKeyToCipher;

      private volatile SecretKeySpec lazyKeySpec;

      @Builder
      public SecretKeyCipher(
          @NonNull Function<String, SecretKeySpec> passwordToSecretKeySpec,
          @NonNull BiFunction<SecretKeySpec, Integer, Cipher> secretKeyToCipher) {
        this.passwordToSecretKeySpec = passwordToSecretKeySpec;
        this.secretKeyToCipher = secretKeyToCipher;
      }

      @Override
      public Cipher createInitializedCipher(String password, int cipherMode) {
        return secretKeyToCipher.apply(getOrCreateKeySpec(password), cipherMode);
      }

      private SecretKeySpec getOrCreateKeySpec(String password) {
        if (lazyKeySpec == null) {
          synchronized (this) {
            if (lazyKeySpec == null) {
              lazyKeySpec = passwordToSecretKeySpec.apply(password);
            }
          }
        }
        return lazyKeySpec;
      }
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

  public static class UrlSafeEncodings {
    public static UrlSafeEncoding base32() {
      return new Base32UrlSafeEncoding();
    }

    public static UrlSafeEncoding base62() {
      return new Base62UrlSafeEncoding();
    }

    private static class Base32UrlSafeEncoding implements UrlSafeEncoding {
      /**
       * We'll use Base 32 (https://www.ietf.org/rfc/rfc4648.txt) to encode. This uses characters
       * A-Z and numbers 2-7, making this URL safe and _reasonable_ nice looking. We'll also pad
       * with 0 instead of the standard = character, to keep the ID looking nice, e.g.
       *
       * <pre>
       * I2-7VES7OYMFUBMJCV4OO5S6JNDSJMJL2IOZOJQK45OHMKPO2ZWWZUA0000
       * </pre>
       *
       * The downside is that these IDs are long, ~60 characters. However, the base 64 versions are
       * around ~48 characters and are much more _ugly_.
       */
      private static final Base32 BASE = new Base32(1024, "".getBytes(UTF_8), false, (byte) '0');

      @Override
      public byte[] decode(String encoded) {
        return BASE.decode(encoded.getBytes(UTF_8));
      }

      @Override
      public String encode(byte[] unencodedBytes) {
        return new String(BASE.encode(unencodedBytes), UTF_8);
      }
    }

    private static class Base62UrlSafeEncoding implements UrlSafeEncoding {
      private static final Base62 BASE = Base62.createInstance();

      @Override
      public byte[] decode(String encoded) {
        return BASE.decode(encoded.getBytes(UTF_8));
      }

      @Override
      public String encode(byte[] unencodedBytes) {
        return new String(BASE.encode(unencodedBytes), UTF_8);
      }
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
