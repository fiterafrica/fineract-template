/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.service;

import java.util.Properties;
import javax.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.data.SMTPCredentialsData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GmailBackedPlatformEmailService implements PlatformEmailService {

    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;

    @Autowired
    public GmailBackedPlatformEmailService(final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
    }

    @Override
    public void sendToUserAccount(String organisationName, String contactName, String address, String username, String unencodedPassword) {

        final String subject = "Welcome to Inkomoko Core Banking System";

        final StringBuilder builder = new StringBuilder(10);
        builder.append("<html>").append("<body>").append("<p>Dear <strong>").append(contactName).append("</strong>,</p>")
                .append("<p>Your CBS account has been created. Below are your access credentials:</p>")
                .append("<ul><li><strong>System access link:</strong> <a href=\"https://www.cbs.inkomoko.com\">https://www.cbs.inkomoko.com</a></li>")
                .append("<li><strong>Username:</strong> <em>").append(username).append("</em></li><li><strong>Password:</strong> <em>")
                .append(unencodedPassword).append("</em></li>").append("</ul>").append("<p>Please log in and change your password.</p>")
                .append("<p><strong>Note:</strong> Your password has to include uppercase letters, lowercase letters, numbers, and special characters.</p>")
                .append("<p>If you have any questions, kindly reach out to our <a href=\"https://inkomoko.freshservice.com/support/tickets/new\">support team</a></p>")
                .append("<p>Best regards,<br>Inkomoko Team</p>").append("</body>").append("</html>");

        final EmailDetail emailDetail = new EmailDetail(subject, builder.toString(), address, contactName);
        sendDefinedEmail(emailDetail);

    }

    @Override
    public void sendDefinedEmail(EmailDetail emailDetails) {
        final SMTPCredentialsData smtpCredentialsData = this.externalServicesReadPlatformService.getSMTPCredentials();

        final String authuser = smtpCredentialsData.getUsername();
        final String authpwd = smtpCredentialsData.getPassword();

        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpCredentialsData.getHost()); // smtp.gmail.com
        mailSender.setPort(Integer.parseInt(smtpCredentialsData.getPort())); // 587

        // Important: Enable less secure app access for the gmail account used in the following authentication

        mailSender.setUsername(authuser); // use valid gmail address
        mailSender.setPassword(authpwd); // use password of the above gmail account

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "false");

        // these are the added lines
        props.put("mail.smtp.starttls.enable", "true");
        // props.put("mail.smtp.ssl.enable", "true");

        props.put("mail.smtp.socketFactory.port", Integer.parseInt(smtpCredentialsData.getPort()));
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");// NOSONAR
        props.put("mail.smtp.socketFactory.fallback", "true");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpCredentialsData.getFromEmail()); // same email address used for the authentication
            helper.setTo(emailDetails.getAddress());
            helper.setSubject(emailDetails.getSubject());
            helper.setText(emailDetails.getBody(), true); // 'true' indicates HTML content

            mailSender.send(message);

            log.info("Email successfully sent to: {}. Subject: {}", emailDetails.getAddress(), emailDetails.getSubject());

        } catch (Exception e) {
            log.error("Email failed sending to: {}. Subject: {}", emailDetails.getAddress(), emailDetails.getSubject());
            throw new PlatformEmailSendException(e);
        }
    }
}
