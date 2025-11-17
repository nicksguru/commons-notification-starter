package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.AlertTransport;
import guru.nicks.commons.notification.service.AlertService;
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
public class AlertServiceImpl<T> implements AlertService<T> {

    private final List<? extends AlertTransport<T>> transports;

    /**
     * Constructor.
     *
     * @throws IllegalStateException no alert transports defined
     */
    public AlertServiceImpl(Collection<? extends AlertTransport<T>> transports) {
        if (CollectionUtils.isEmpty(transports)) {
            throw new IllegalStateException("No alert transports defined");
        }

        // immutability + preserved order
        this.transports = transports.stream()
                .distinct()
                .toList();

        // unwrap class names beneath JdkProxy instances
        log.info("Alert transports: {}",
                TransformUtils.toList(this.transports, AopUtils::getTargetClass, Class::getName));
    }

    @Override
    public boolean send(T category, String message, Map<String, ?> messageContext) {
        List<Supplier<Pair<Class<?>, RuntimeException>>> senders =
                TransformUtils.toList(transports, transport ->
                        () -> sendViaTransport(category, transport, message, messageContext));
        List<Pair<Class<?>, RuntimeException>> results = FutureUtils.getInParallel(senders);

        long failureCount = results.stream()
                .filter(pair -> pair.getValue() != null)
                .count();

        if (failureCount == results.size()) {
            log.error("Alert not sent, all transports failed: {}", results);
            return false;
        }

        if (failureCount > 0) {
            log.warn("Alert sent, but some transports failed: {}", results);
        } else {
            log.debug("Alert sent, all transports succeeded: {}", results);
        }

        return true;
    }

    /**
     * Sends an alert message using a single transport. Wraps the send operation in a try-catch block to gracefully
     * handle any exceptions that may occur during the process.
     *
     * @param category         alert category.
     * @param transport        transport to use for sending the alert
     * @param message          alert message content
     * @param messageVariables variables for message templating or context
     * @return pair (transport class, {@link RuntimeException})
     */
    protected Pair<Class<?>, RuntimeException> sendViaTransport(T category,
            AlertTransport<T> transport, String message, Map<String, ?> messageVariables) {
        var clazz = AopUtils.getTargetClass(transport.getClass());

        try {
            transport.send(category, message, messageVariables);
            return Pair.of(clazz, null);
        } catch (RuntimeException e) {
            return Pair.of(clazz, e);
        }
    }

}
