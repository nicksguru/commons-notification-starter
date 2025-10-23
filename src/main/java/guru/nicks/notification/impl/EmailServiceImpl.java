package guru.nicks.notification.impl;

import guru.nicks.notification.service.EmailService;
import guru.nicks.service.FreemarkerTemplateService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;

/**
 * Renders template with {@link FreemarkerTemplateService} and sends message via {@link JavaMailSender} (configured
 * elsewhere). Leverages retries and circuit breaking using
 * <a href="https://resilience4j.readme.io/docs/getting-started-3#configuration">Resilience4j configuration</a>
 * {@value #RESILIENCE4J_CONFIG_NAME}, if any (otherwise, default values apply).
 */
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    public static final String RESILIENCE4J_CONFIG_NAME = "sendEmail";

    // DI
    private final JavaMailSender mailSender;
    private final FreemarkerTemplateService templateService;

    @Retry(name = RESILIENCE4J_CONFIG_NAME)
    @CircuitBreaker(name = RESILIENCE4J_CONFIG_NAME)
    @Override
    public void sendHtmlWithTemplate(String from, String to, String subject,
            String templateName, Map<?, ?> templateContext) {
        String body = templateService.render(templateName, templateContext);
        sendHtml(from, to, subject, body);
    }

    @Retry(name = RESILIENCE4J_CONFIG_NAME)
    @CircuitBreaker(name = RESILIENCE4J_CONFIG_NAME)
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
