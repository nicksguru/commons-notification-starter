package guru.nicks.notification.service;

import guru.nicks.notification.AlertTransport;
import guru.nicks.utils.ExceptionUtils;

import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Implementations are supposed to send alerts via {@link AlertTransport}'s passed to their constructors. Also, they are
 * supposed to not throw exceptions if all the transports fail - alerts should not affect the business logic.
 *
 * @param <T> alert category type
 */
public interface AlertService<T extends Enum<T>> {

    /**
     * Sends message via all {@link AlertTransport}'s and considers it sent if at least one transport succeeds.
     *
     * @param category       alert category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     * @return {@code true} if the message has been sent successfully via at least one transport
     */
    boolean send(T category, String message, Map<String, ?> messageContext);

    /**
     * Sends message via all {@link AlertTransport}'s and considers it sent if at least one transport succeeds.
     *
     * @param category alert category
     * @param message  message text
     * @return {@code true} if the message has been sent successfully via at least one transport
     */
    default boolean send(T category, String message) {
        return send(category, message, Collections.emptyMap());
    }

    /**
     * Sends message via all {@link AlertTransport}'s and considers it sent if at least one transport succeeds.
     *
     * @param category       alert category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     * @param t              exception
     * @return {@code true} message sent
     */
    default boolean send(T category, String message, Map<String, ?> messageContext, @Nullable Throwable t) {
        return send(category, message + ": " + ExceptionUtils.formatWithCompactStackTrace(t), messageContext);
    }

    /**
     * Sends message via all {@link AlertTransport}'s and considers it sent if at least one transport succeeds.
     *
     * @param category alert category
     * @param message  message text
     * @param t        exception
     * @return {@code true} message sent
     */
    default boolean send(T category, String message, @Nullable Throwable t) {
        return send(category, message, Collections.emptyMap(), t);
    }

}
