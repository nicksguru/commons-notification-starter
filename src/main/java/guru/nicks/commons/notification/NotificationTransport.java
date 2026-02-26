package guru.nicks.commons.notification;

import guru.nicks.commons.utils.Resilience4jUtils;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Notification transports may filter out the messages by category, apply throttling, etc. Also, they may fail.
 * <p>
 * This base class supports circuit breaking and rate limiting. Subclasses must implement
 * {@link #sendRaw(NotificationCategory, String, Map)} which will be decorated with these patterns.
 *
 * @param <T> notification category type
 */
@Slf4j
public abstract class NotificationTransport<T extends NotificationCategory> {

    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;

    /**
     * Constructor.
     *
     * @param rateLimiter    rate limiter, can be {@code null} or e.g.
     *                       {@link Resilience4jUtils#createDefaultRateLimiter(String)}
     * @param circuitBreaker circuit breaker, can be {@code null} or e.g.
     *                       {@link Resilience4jUtils#createDefaultCircuitBreaker(String)}
     */
    protected NotificationTransport(@Nullable RateLimiter rateLimiter, @Nullable CircuitBreaker circuitBreaker) {
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Sends notification.
     *
     * @param category       notification category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     * @throws RequestNotPermitted       rate limit exceeded (if rate limiter is configured)
     * @throws CallNotPermittedException circuit breaker is not open (if circuit breaker is configured)
     */
    public void send(T category, String message, Map<String, ?> messageContext) {
        if ((rateLimiter == null) && (circuitBreaker == null)) {
            sendRaw(category, message, messageContext);
            return;
        }

        var decorator = Decorators.ofRunnable(() -> sendRaw(category, message, messageContext));

        // circuit breaker is mentioned FIRST, therefore called LAST - after rate limiter permits the call
        if (circuitBreaker != null) {
            decorator = decorator.withCircuitBreaker(circuitBreaker);
        }

        // rate limiter is mentioned LAST, therefore called FIRST - before circuit breaker
        if (rateLimiter != null) {
            decorator = decorator.withRateLimiter(rateLimiter);
        }

        decorator.run();
    }

    /**
     * Sends notification without decorations. This method is called by {@link #send(NotificationCategory, String, Map)}
     * after applying optional legacy rate limiting, Resilience4j rate limiting, and optional circuit breaking.
     *
     * @param category       notification category
     * @param message        message text
     * @param messageContext key/value context to append after the message text
     */
    protected abstract void sendRaw(T category, String message, Map<String, ?> messageContext);

}
