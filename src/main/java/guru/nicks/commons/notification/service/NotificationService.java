package guru.nicks.commons.notification.service;

import guru.nicks.commons.feature.FeatureTester;
import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.utils.ExceptionUtils;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.togglz.core.Feature;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Implementations are supposed to send notifications via {@link NotificationTransport}'s passed to their constructors.
 * Also, they are supposed to NOT throw exceptions if some/all the transports fail - notifications should not affect the
 * business logic.
 *
 * @param <T> message category type
 */
public interface NotificationService<T extends NotificationCategory> {

    /**
     * Builds a notifier that either {@link #send(NotificationCategory, String, Throwable) alerts} or
     * {@link Logger#error(String, Throwable) logs}, depending on feature state. The intended call sites are Spring bean
     * constructors and {@link PostConstruct @PostConstruct}.
     *
     * @param feature        feature whose state decides alert vs. log in EACH method call (feature states are
     *                       volatile), requires {@link FeatureTester} bean
     * @param category       alert category
     * @param fallbackLogger caller's logger, invoked when the feature is disabled
     * @return consumer that alerts (feature on) or logs (feature off) the message &amp; its cause
     */
    BiConsumer<String, Throwable> wrapErrorNotifier(Feature feature, T category, Logger fallbackLogger);

    /**
     * Sends message via all available {@link NotificationTransport}'s and considers it sent if at least one transport
     * succeeds.
     *
     * @param category       message category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     * @return {@code true} if the message has been sent successfully via at least one transport
     */
    boolean send(T category, String message, Map<String, ?> messageContext);

    /**
     * Sends message via all available {@link NotificationTransport}'s and considers it sent if at least one transport
     * succeeds.
     *
     * @param category message category
     * @param message  message text
     * @return {@code true} if the message has been sent successfully via at least one transport
     */
    default boolean send(T category, String message) {
        return send(category, message, Collections.emptyMap());
    }

    /**
     * Sends message via all available {@link NotificationTransport}'s and considers it sent if at least one transport
     * succeeds.
     *
     * @param category       message category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     * @param t              exception
     * @return {@code true} message sent
     */
    default boolean send(T category, String message, Map<String, ?> messageContext, @Nullable Throwable t) {
        return send(category, message + ": " + ExceptionUtils.formatWithCompactStackTrace(t), messageContext);
    }

    /**
     * Sends message via all available {@link NotificationTransport}'s and considers it sent if at least one transport
     * succeeds.
     *
     * @param category message category
     * @param message  message text
     * @param t        exception
     * @return {@code true} message sent
     */
    default boolean send(T category, String message, @Nullable Throwable t) {
        return send(category, message, Collections.emptyMap(), t);
    }

}
