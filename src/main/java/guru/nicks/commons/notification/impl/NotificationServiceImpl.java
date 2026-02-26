package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.notification.service.NotificationService;
import guru.nicks.commons.utils.FutureUtils;
import guru.nicks.commons.utils.TransformUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.aop.support.AopUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class NotificationServiceImpl<T extends NotificationCategory> implements NotificationService<T> {

    private final List<? extends NotificationTransport<T>> transports;

    /**
     * Constructor.
     *
     * @param transports notification transports
     * @throws IllegalArgumentException no notification transports defined
     */
    public NotificationServiceImpl(Collection<? extends NotificationTransport<T>> transports) {
        if (CollectionUtils.isEmpty(transports)) {
            throw new IllegalArgumentException("No notification transports defined");
        }

        // immutability + preserved order
        this.transports = transports.stream()
                .distinct()
                .toList();

        // unwrap class names beneath JdkProxy instances
        log.info("Notification transports: {}",
                TransformUtils.toList(this.transports, AopUtils::getTargetClass, Class::getName));
    }

    @Override
    public boolean send(T category, String message, Map<String, ?> messageContext) {
        List<Supplier<Pair<Class<?>, RuntimeException>>> senders =
                TransformUtils.toList(transports, transport ->
                        () -> sendViaTransport(transport, category, message, messageContext));
        List<Pair<Class<?>, RuntimeException>> results = FutureUtils.getInParallel(senders);

        // format results for logging: TransportClass[OK] or TransportClass[ERROR: message]
        List<String> textResults = results.stream()
                .map(pair -> pair.getLeft().getName()
                        + ((pair.getRight() == null)
                        ? "[OK]"
                        : "[ERROR: " + pair.getValue().getMessage() + "]"))
                .toList();

        long failureCount = results.stream()
                .filter(pair -> pair.getValue() != null)
                .count();

        if (failureCount == results.size()) {
            log.error("Notification not sent, all transports failed: {}", textResults);
            return false;
        }

        if (failureCount > 0) {
            log.warn("Notification sent, but some transports failed: {}", textResults);
        } else {
            log.debug("Notification sent, all transports succeeded: {}", textResults);
        }

        return true;
    }

    /**
     * Sends a notification message using a single transport. Wraps the send operation in a try-catch block to
     * gracefully handle any exceptions that may occur during the process.
     *
     * @param transport        transport to use for sending the notification
     * @param category         message category
     * @param message          message content
     * @param messageVariables variables for message templating or context
     * @return pair (transport class, {@link RuntimeException})
     */
    protected Pair<Class<?>, RuntimeException> sendViaTransport(NotificationTransport<T> transport,
            T category, String message, Map<String, ?> messageVariables) {
        try {
            transport.send(category, message, messageVariables);
            return Pair.of(transport.getClass(), null);
        } catch (RuntimeException e) {
            return Pair.of(transport.getClass(), e);
        }
    }

}
