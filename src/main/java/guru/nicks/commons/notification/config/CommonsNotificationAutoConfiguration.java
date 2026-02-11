package guru.nicks.commons.notification.config;

import guru.nicks.commons.notification.impl.EmailServiceImpl;
import guru.nicks.commons.notification.service.EmailService;
import guru.nicks.commons.service.FreemarkerTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class CommonsNotificationAutoConfiguration {

    /**
     * Creates {@link EmailService} bean if it's not already present.
     */
    @ConditionalOnMissingBean(EmailService.class)
    @Bean
    public EmailService emailService(JavaMailSender mailSender, FreemarkerTemplateService templateService) {
        log.debug("Building {} bean", EmailService.class.getSimpleName());
        return new EmailServiceImpl(mailSender, templateService);
    }

}
