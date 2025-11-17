package guru.nicks.commons.notification.service;

import java.util.Map;

/**
 * Implementations are encouraged to leverage retries and circuit breaking.
 */
public interface EmailService {

    /**
     * Sends message.
     *
     * @param from            'from' address
     * @param to              'to' addresses (comma-separated)
     * @param subject         message subject
     * @param templateName    template name
     * @param templateContext variables to pass to template
     */
    void sendHtmlWithTemplate(String from, String to, String subject, String templateName, Map<?, ?> templateContext);

    /**
     * Sends message.
     *
     * @param from    'from' address
     * @param to      'to' addresses (comma-separated)
     * @param subject message subject
     * @param body    message body
     */
    void sendHtml(String from, String to, String subject, String body);

}
