package com.ncop.clientmaster.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender emailSender;

    public void sendRegistrationEmail(String toEmail, String companyName, String registrationLink) {
        if (emailSender == null) {
            log.warn("JavaMailSender is not configured. Mocking email to: {}", toEmail);
            log.info("Subject: Action Required: Complete your Client Registration for {}", companyName);
            log.info("Body: Please complete your registration and upload required documents by visiting: {}", registrationLink);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@ncop.com");
            message.setTo(toEmail);
            message.setSubject("Action Required: Complete your Client Registration for " + companyName);
            message.setText("Dear Client,\n\nPlease complete your registration, provide your bank details and upload the required documents by visiting the following link:\n\n" + registrationLink + "\n\nThank you,\nTeam NCOP");
            emailSender.send(message);
            log.info("Registration email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
        }
    }
}
