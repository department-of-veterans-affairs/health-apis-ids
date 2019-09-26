package gov.va.api.health.ids.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import gov.va.api.health.ids.api.ResourceIdentity;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * This ID encoder uses encryption as a means for grouping and obscuring data. It is not meant to
 * provide strong encryption or protect secrets. It's goal is encapsulate resource identity
 * information in a form that saves system, resource, and identity in a form that can be used in
 * URLS, etc.
 */
public class EncryptingIdEncoder implements IdEncoder {

  private final SecretKeySpec secretKey;

  private final CipherPool ciphers = new CipherPool();

  @Builder
  @SneakyThrows
  EncryptingIdEncoder(@NonNull String password) {
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
  @SneakyThrows
  public ResourceIdentity decode(String encoded) {
    byte[] encryptedBytes = Base64.getUrlDecoder().decode(encoded.getBytes(UTF_8));
    byte[] decryptedBytes = ciphers.decryptor().doFinal(encryptedBytes);
    String delimitedIdentity = new String(decryptedBytes, UTF_8);
    return DelimitedRepresentation.from(delimitedIdentity);
  }

  @Override
  @SneakyThrows
  public String encode(ResourceIdentity resourceIdentity) {
    String delimitedIdentity = DelimitedRepresentation.to(resourceIdentity);
    byte[] decryptedBytes = delimitedIdentity.getBytes(UTF_8);
    byte[] encryptedBytes = ciphers.encryptor().doFinal(decryptedBytes);
    return new String(Base64.getUrlEncoder().encode(encryptedBytes), UTF_8);
  }

  /**
   * This encapsulates the logic for representing the resource identity as a simple string. These
   * are the unencrypted values of the ID.
   */
  static class DelimitedRepresentation {

    private static final String DELIMITER = ":";

    static ResourceIdentity from(String delimited) {
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
          .system(system)
          .resource(resource)
          .identifier(identity)
          .build();
    }

    static String to(ResourceIdentity resourceIdentity) {
      if (isBlank(resourceIdentity.system())
          || isBlank(resourceIdentity.resource())
          || isBlank(resourceIdentity.identifier())) {
        throw new IncompleteResourceIdentity(resourceIdentity);
      }
      return resourceIdentity.system()
          + DELIMITER
          + resourceIdentity.resource()
          + DELIMITER
          + resourceIdentity.identifier();
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

  /**
   * Ciphers are expensive to create and initialize. They are re-usable, but not thread safe. We'll
   * use a cypher-per-thread approach. We'll also need to keep encrypting and decrypting separate
   * because they are initialized differently.
   */
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
