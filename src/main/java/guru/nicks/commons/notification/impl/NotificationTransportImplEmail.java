package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.notification.service.EmailService;
import guru.nicks.commons.utils.TransformUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Email transport implementation.
 *
 * @param <T> notification category type
 */
public class NotificationTransportImplEmail<T extends NotificationCategory> implements NotificationTransport<T> {

    private final EmailService emailService;
    private final String messageSubject;
    private final String from;
    private final String to;
    private final String templateName;

    /**
     * Constructor.
     *
     * @param emailService email service
     * @param originator   message originator, such as application name, must not be blank
     * @param from         'from' address
     * @param to           'to' addresses (comma-separated)
     * @param templateName template name for
     *                     {@link EmailService#sendHtmlWithTemplate(String, String, String, String, Map)}
     */
    public NotificationTransportImplEmail(EmailService emailService, String originator, String from, String to,
            String templateName) {

        this.emailService = checkNotNull(emailService, "emailService");
        this.messageSubject = checkNotBlank(originator, "messageSubject");
        this.from = checkNotBlank(from, "from");
        this.to = checkNotBlank(to, "to");
        this.templateName = checkNotBlank(templateName, "templateName");
    }

    @Override
    public void send(T category, String message, Map<String, ?> messageContext) {
        checkNotNull(category, "category");

        // Freemarker demands that, if map is to be iterated in template, all of its values be strings
        Map<String, String> contextWithStringValues = (messageContext == null)
                ? Collections.emptyMap()
                : messageContext.entrySet()
                        .stream()
                        .filter(mapEntry -> mapEntry.getKey() != null)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                mapEntry -> TransformUtils.stringify(mapEntry.getValue(), true)));

        var context = (messageContext == null)
                ? new HashMap<String, Object>()
                : new HashMap<String, Object>(messageContext);
        context.put("title", category.format(messageSubject));
        context.put("message", message);
        context.put("context", contextWithStringValues);

        emailService.sendHtmlWithTemplate(from, to,
                Objects.toString(context.get("title"), ""),
                templateName, context);
    }

}
