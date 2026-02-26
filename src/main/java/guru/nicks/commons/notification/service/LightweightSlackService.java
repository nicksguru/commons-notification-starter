package guru.nicks.commons.notification.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.util.Map;

/**
 * Implementations are encouraged to leverage retries and circuit breaking.
 */
public interface LightweightSlackService {

    /**
     * Sends JSON to Slack according to their
     * <a href="https://api.slack.com/messaging/webhooks#advanced_message_formatting">format spec</a>.
     *
     * @param map to be converted to JSON
     * @throws CallNotPermittedException if circuit breaker is not open
     * @throws RuntimeException          if retries fail (this is the original exception from the underlying code)
     */
    void send(Map<String, ?> map);

    /**
     * Sends plain text message to Slack.
     *
     * @param text text to send
     */
    void sendPlainText(String text);

    /**
     * Sends Markdown-enriched text message to Slack.
     *
     * @param title message title (plain text, never {@code null}, can contain emojis)
     * @param text  Markdown (or just plain text), never {@code null}
     */
    void sendMarkdown(String title, String text);

}
