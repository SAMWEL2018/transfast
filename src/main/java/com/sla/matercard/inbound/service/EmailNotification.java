package com.sla.matercard.inbound.service;


import com.sla.matercard.inbound.Utility.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * @author samwel.wafula
 * Created on 07/03/2024
 * Time 10:56
 * Project MoneyTrans
 */

@Service()
@Slf4j
@RequiredArgsConstructor
public class EmailNotification {

    public String subject = "MASTERCARD_INBOUND PROCESSING ERROR";


    private final AppConfig config;

    public Object sendNotification(String subject, String msg) {
        this.subject = subject;
        return sendNotification(msg, false);
    }

    public Object sendNotification(String msg) {
        return sendNotification(msg, true);
    }

    public Object sendNotification(String msg, boolean isError) {

        String emails;
        if (!isError) {
            emails = config.getSendOps();
        } else {
            emails = config.getSendTechOps();
        }

        final Session newSession = Session.getInstance(this.Mail_Properties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
            }
        });

        try {
            String[] recipient = emails.split(",");

            InternetAddress[] internetAddresses = new InternetAddress[recipient.length];

            for (int i = 0; i < recipient.length; i++) {
                internetAddresses[i] = new InternetAddress(recipient[i]);
            }

            final Message EmailMessage = new MimeMessage(newSession);
            EmailMessage.addRecipients(Message.RecipientType.TO, internetAddresses);
            EmailMessage.setFrom(new InternetAddress(config.getSmtpUsername()));
            EmailMessage.setSubject(this.subject); // email subject
            EmailMessage.setContent(msg, "text/html"); // The content of email
            EmailMessage.setSentDate(new Date());
            Transport.send(EmailMessage);// Transport the email
            log.info("Your Email has been sent successfully!");
        } catch (final MessagingException e) { // exception to catch the errors
            log.error("Email Sending Failed"); // failed
            e.getCause();
        }
        return null;
    }

    public Properties Mail_Properties() {
        final Properties Mail_Prop = new Properties();
        Mail_Prop.put("mail.smtp.host", config.getSmtpHost());
        Mail_Prop.put("mail.smtp.post", "587");
        Mail_Prop.put("mail.smtp.auth", true);
        Mail_Prop.put("mail.smtp.starttls.enable", true);
        Mail_Prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        return Mail_Prop;
    }

}
