package taxicab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String recipientEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("SwiftRidef - Email Verification Code");

        message.setText(
                "Welcome to SwiftRide!\n\n"
                        + "Your email verification code is: " + otp + "\n\n"
                        + "This code expires in 5 minutes.\n"
                        + "If you did not request this code, please ignore this email."
        );

        mailSender.send(message);
    }
}
