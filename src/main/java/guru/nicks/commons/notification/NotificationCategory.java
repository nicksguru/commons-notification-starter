package guru.nicks.commons.notification;

import org.slf4j.event.Level;

import java.util.Locale;

/**
 * Notification category.
 */
public interface NotificationCategory {

    Level getLogLevel();

    /**
     * @return description of the category, such as 'Remote call failed'.
     */
    String getDescription();

    /**
     * Formats fields: <code>[logLevel] originator - description</code>.
     *
     * @param originator message originator, such as application name
     * @return string, can be used as a message title
     */
    default String format(String originator) {
        return String.format(Locale.US, "[%s] %s - %s",
                getLogLevel().toString().toLowerCase(),
                originator,
                getDescription());
    }

}
