package guru.nicks.notification;

import java.util.Map;

/**
 * Transport for sending alerts. The idea is to send each alert via all available transports. Some may filter out the
 * messages by their category, apply API throttling, etc. Also, some may fail.
 * <p>
 * Implementations are encouraged to leverage retries and a circuit breaker.
 *
 * @param <T> alert category type
 */
public interface AlertTransport<T> {

    /**
     * Sends alert.
     *
     * @param category       alert category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     */
    void send(T category, String message, Map<String, ?> messageContext);

}
