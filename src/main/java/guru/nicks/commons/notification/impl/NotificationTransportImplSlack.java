package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.notification.service.LightweightSlackService;
import guru.nicks.commons.utils.Resilience4jUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.annotation.Nullable;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Slack transport implementation.
 *
 * @param <T> notification category type
 */
public class NotificationTransportImplSlack<T extends NotificationCategory> extends NotificationTransport<T> {

    private final LightweightSlackService slackService;
    private final ObjectMapper objectMapper;
    private final String originator;

    /**
     * Constructor.
     *
     * @param rateLimiter    rate limiter, can be {@code null} or e.g.
     *                       {@link Resilience4jUtils#createDefaultRateLimiter(String)}
     * @param circuitBreaker circuit breaker, can be {@code null} or e.g.
     *                       {@link Resilience4jUtils#createDefaultCircuitBreaker(String)}
     * @param slackService   Slack service
     * @param objectMapper   Jackson object mapper
     * @param originator     message originator, such as application name, must not be blank
     */
    public NotificationTransportImplSlack(@Nullable RateLimiter rateLimiter, @Nullable CircuitBreaker circuitBreaker,
            LightweightSlackService slackService, ObjectMapper objectMapper, String originator) {
        super(rateLimiter, circuitBreaker);
        this.slackService = checkNotNull(slackService, "slackService");
        this.objectMapper = checkNotNull(objectMapper, "objectMapper");
        this.originator = checkNotBlank(originator, "originator");
    }

    @Override
    protected void sendRaw(T category, String message, Map<String, ?> messageContext) {
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
