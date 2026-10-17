package taxicab.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpVerificationService {

    private final SecureRandom random = new SecureRandom();

    private final ConcurrentHashMap<String, OtpData> otpStorage =
            new ConcurrentHashMap<>();

    public String generateOtp(String email, String role) {
        String key = createKey(email, role);

        String otp = String.format("%06d", random.nextInt(1_000_000));

        Instant expiryTime = Instant.now().plusSeconds(300);

        otpStorage.put(key, new OtpData(otp, expiryTime));

        return otp;
    }

    public boolean verifyOtp(String email, String role, String enteredOtp) {
        if (email == null || role == null || enteredOtp == null) {
            return false;
        }

        String key = createKey(email, role);
        OtpData data = otpStorage.get(key);

        if (data == null) {
            return false;
        }

        if (Instant.now().isAfter(data.expiryTime)) {
            otpStorage.remove(key);
            return false;
        }

        if (!data.otp.equals(enteredOtp.trim())) {
            return false;
        }

        otpStorage.remove(key);
        return true;
    }

    public void removeOtp(String email, String role) {
        if (email != null && role != null) {
            otpStorage.remove(createKey(email, role));
        }
    }

    private String createKey(String email, String role) {
        return role.trim().toUpperCase() + ":" +
                email.trim().toLowerCase();
    }

    private static class OtpData {
        private final String otp;
        private final Instant expiryTime;

        private OtpData(String otp, Instant expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}
