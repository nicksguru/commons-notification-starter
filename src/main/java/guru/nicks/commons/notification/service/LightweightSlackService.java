package guru.nicks.commons.notification.service;

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
