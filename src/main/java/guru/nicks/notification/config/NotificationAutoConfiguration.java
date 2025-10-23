package guru.nicks.notification.config;

import guru.nicks.notification.impl.EmailServiceImpl;
import guru.nicks.notification.service.EmailService;
import guru.nicks.service.FreemarkerTemplateService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
public class NotificationAutoConfiguration {

    @ConditionalOnMissingBean(EmailService.class)
    @Bean
    public EmailService emailService(JavaMailSender mailSender, FreemarkerTemplateService templateService) {
        return new EmailServiceImpl(mailSender, templateService);
    }

}
