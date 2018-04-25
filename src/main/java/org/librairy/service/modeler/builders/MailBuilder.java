package org.librairy.service.modeler.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

@Component
public class MailBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MailBuilder.class);


    public void newMailTo(String dest){
        final String username = "librairy.framework@gmail.com";
        final String password = "librairyoeg2016";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(dest));
            message.setSubject("Model Trained");
            message.setText("Your Topic Model is ready!,"
                    + "\n\n Check the result here: ");

            Transport.send(message);

            System.out.println("Mail sent!");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
