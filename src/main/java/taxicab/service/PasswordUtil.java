package taxicab.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

public final class PasswordUtil {

    private static final int ITERATIONS = 60000;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            byte[] result = createHash(password, salt);

            return Base64.getEncoder().encodeToString(salt)
                    + ":"
                    + Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Password could not be protected", e);
        }
    }

    public static boolean matches(String password, String storedValue) {
        try {
            String[] parts = storedValue.split(":");

            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual = createHash(password, salt);

            return MessageDigest.isEqual(expected, actual);

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] createHash(String password, byte[] salt)
            throws Exception {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } finally {
            spec.clearPassword();
        }
    }
}