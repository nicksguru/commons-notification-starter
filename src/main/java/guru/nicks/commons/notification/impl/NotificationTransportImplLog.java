package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.utils.TransformUtils;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.Locale;
import java.util.Map;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Appends messages to application log. Should always be enabled - at least one (fallback) transport should exist.
 *
 * @param <T> notification category type
 */
@Slf4j
public class NotificationTransportImplLog<T extends NotificationCategory> implements NotificationTransport<T> {

    @Override
    public void send(T category, @Nullable String message, Map<String, ?> messageContext) {
        checkNotNull(category, "category");
        var context = "";

        if (!MapUtils.isEmpty(messageContext)) {
            context = " (" + TransformUtils.stringify(messageContext, false) + ")";
        }

        var text = String.format(Locale.US, "%s: %s%s", category.getDescription(), message, context);

        switch (category.getLogLevel()) {
            case ERROR:
                log.error(text);
                break;

            case WARN:
                log.warn(text);
                break;

            case DEBUG:
                log.debug(text);
                break;

            case TRACE:
                log.trace(text);
                break;

            default:
                log.info(text);
                break;
        }

    }

}
