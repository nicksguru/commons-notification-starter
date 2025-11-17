package guru.nicks.commons.notification.config;

import guru.nicks.commons.notification.impl.EmailServiceImpl;
import guru.nicks.commons.notification.service.EmailService;
import guru.nicks.commons.service.FreemarkerTemplateService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
public class CommonsNotificationAutoConfiguration {

    @ConditionalOnMissingBean(EmailService.class)
    @Bean
    public EmailService emailService(JavaMailSender mailSender, FreemarkerTemplateService templateService) {
        return new EmailServiceImpl(mailSender, templateService);
    }

}
