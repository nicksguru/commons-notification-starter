package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.service.EmailService;
import guru.nicks.commons.service.FreemarkerTemplateService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;

/**
 * Renders templates with {@link FreemarkerTemplateService} and sends messages via {@link JavaMailSender}. Subclasses
 * are encouraged to create Spring beans and annotate them with rate limiting and circuit breaking annotations.
 */
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    // DI
    private final JavaMailSender mailSender;
    private final FreemarkerTemplateService templateService;

    @Override
    public void sendHtmlWithTemplate(String from, String to, String subject,
            String templateName, Map<?, ?> templateContext) {
        String body = templateService.render(templateName, templateContext);
        sendHtml(from, to, subject, body);
    }

    @Override
    public void sendHtml(String from, String to, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        try {
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Failed to construct email message: " + e.getMessage(), e);
        }

        // In HTML, linebreaks bear no meaning and usually can be removed to reduce message size. However, if a template
        // has '<pre>', this must not be done.
        if (log.isTraceEnabled()) {
            log.trace("Sending email message: to='{}', subject='{}', body='{}'", to, subject, body);
        } else {
            log.info("Sending email message (log level 'trace' will reveal potentially confidential message content): "
                    + "to='{}', subject='{}'", to, subject);
        }

        mailSender.send(message);
    }

}
