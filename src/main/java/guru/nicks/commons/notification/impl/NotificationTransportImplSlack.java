package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.notification.service.LightweightSlackService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Slack transport implementation.
 *
 * @param <T> notification category type
 */
public class NotificationTransportImplSlack<T extends NotificationCategory> implements NotificationTransport<T> {

    private final LightweightSlackService slackService;
    private final ObjectMapper objectMapper;
    private final String originator;

    /**
     * Constructor.
     *
     * @param slackService Slack service
     * @param objectMapper Jackson object mapper
     * @param originator   message originator, such as application name, must not be blank
     */
    public NotificationTransportImplSlack(LightweightSlackService slackService, ObjectMapper objectMapper,
            String originator) {
        this.slackService = checkNotNull(slackService, "slackService");
        this.objectMapper = checkNotNull(objectMapper, "objectMapper");
        this.originator = checkNotBlank(originator, "originator");
    }

    @Override
    public void send(T category, String message, Map<String, ?> messageContext) {
        checkNotNull(category, "category");
        checkNotBlank(message, "message");
        var text = new StringBuilder(message);

        if (!MapUtils.isEmpty(messageContext)) {
            text.append("\n```\n");

            // pretty-format Map as JSON, on error call toString
            try {
                text.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageContext));
            } catch (Exception e) {
                text.append(messageContext);
            }

            text.append("\n```");
        }

        String title = category.format(originator);

        // add color
        title = switch (category.getLogLevel()) {
            case ERROR -> ":exclamation: " + title;
            case WARN -> ":warning: " + title;
            default -> ":information_source: " + title;
        };

        slackService.sendMarkdown(title, text.toString());
    }

}
