package guru.nicks.commons.notification;

import java.util.Map;

/**
 * Notification transports may filter out the messages by category, apply throttling, etc. Subclasses are encouraged to
 * create Spring beans and annotate them with rate limiting and circuit breaking annotations.
 *
 * @param <T> notification category type
 */
public interface NotificationTransport<T extends NotificationCategory> {

    /**
     * Sends notification.
     *
     * @param category       notification category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     */
    void send(T category, String message, Map<String, ?> messageContext);

}
