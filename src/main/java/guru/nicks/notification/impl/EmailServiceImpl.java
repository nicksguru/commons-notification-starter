package guru.nicks.notification.impl;

import guru.nicks.notification.service.EmailService;
import guru.nicks.service.FreemarkerTemplateService;
import guru.nicks.utils.Resilience4jUtils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;

/**
 * Renders templates with {@link FreemarkerTemplateService} and sends messages via {@link JavaMailSender}. Leverages
 * retries and circuit breaking (see
 * <a href="https://resilience4j.readme.io/docs/getting-started-3#configuration">Resilience4j configuration</a>)
 * by calling {@link #createRetrier()} and {@link #createCircuitBreaker()} from the constructor.
 */
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final FreemarkerTemplateService templateService;

    private final Retry retrier;
    private final CircuitBreaker circuitBreaker;

    public EmailServiceImpl(JavaMailSender mailSender, FreemarkerTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;

        retrier = createRetrier();
        circuitBreaker = createCircuitBreaker();
    }

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

        Decorators.ofRunnable(() -> mailSender.send(message))
                .withRetry(retrier)
                .withCircuitBreaker(circuitBreaker)
                .run();
    }

    /**
     * Creates a {@link Retry} for handling email sending failures. Default implementation calls
     * {@link Resilience4jUtils#createDefaultRetrier(String)} to create a retrier with a default configuration,
     * identified by the name of this class (i.e. possibly a subclass).
     * <p>
     * WARNING: this method is called by the constructor, which means the object state is undefined.
     *
     * @return {@link Retry} instance.
     */
    protected Retry createRetrier() {
        return Resilience4jUtils.createDefaultRetrier(getClass().getName());
    }

    /**
     * Creates a {@link CircuitBreaker} for handling email sending failures. Default implementation calls
     * {@link Resilience4jUtils#createDefaultCircuitBreaker(String)} to create a circuit breaker with a default
     * configuration, identified by the name of this class (i.e. possibly a subclass).
     * <p>
     * WARNING: this method is called by the constructor, which means the object state is undefined.
     *
     * @return {@link CircuitBreaker} instance.
     */
    protected CircuitBreaker createCircuitBreaker() {
        return Resilience4jUtils.createDefaultCircuitBreaker(getClass().getName());
    }

}
