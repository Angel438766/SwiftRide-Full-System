
package taxicab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String recipientEmail, String otp) {

        try {
            if (senderEmail == null || senderEmail.isBlank()) {
                throw new IllegalArgumentException(
                        "Sender email is not configured."
                );
            }

            if (recipientEmail == null
                    || recipientEmail.isBlank()
                    || !recipientEmail.trim().matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException(
                        "Recipient email address is invalid."
                );
            }

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(senderEmail.trim());
            message.setTo(recipientEmail.trim());
            message.setSubject(
                    "SwiftRide - Email Verification Code"
            );

            message.setText(
                    "Welcome to SwiftRide!\n\n"
                            + "Your email verification code is: "
                            + otp + "\n\n"
                            + "This code expires in 5 minutes.\n"
                            + "If you did not request this code, "
                            + "please ignore this email."
            );

            logger.info(
                    "Attempting to send verification email."
            );

            mailSender.send(message);

            logger.info(
                    "Verification email sent successfully."
            );

        } catch (Exception e) {
            logger.error(
                    "Failed to send verification email. "
                            + "Error type: {}. Details: {}",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "Unable to send verification email.", e
            );
        }
    }
}
