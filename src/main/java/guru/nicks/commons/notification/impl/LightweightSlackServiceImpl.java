package guru.nicks.commons.notification.impl;

import guru.nicks.commons.notification.service.LightweightSlackService;

import am.ik.yavi.meta.ConstraintArguments;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestOperations;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Very basic and lightweight REST client for Slack. Can't use Feign because Feign clients themselves may need it for
 * sending error alerts. Resilience4j (with default settings) is leveraged for retries and circuit breaking.
 * <p>
 * This is not a Spring bean because each instance needs a different Slack API URL - to send messages to different Slack
 * channels. Subclasses are encouraged to create Spring beans and annotate them with rate limiting and circuit breaking
 * annotations.
 */
@Slf4j
public class LightweightSlackServiceImpl implements LightweightSlackService {

    /**
     * Slack refuses to send sections where 'text' is longer than this.
     */
    public static final int MAX_TEXT_LENGTH = 3000;

    private static final String TYPE = "type";
    private static final String TEXT = "text";
    private static final String BLOCKS = "blocks";
    private static final String MRKDWN = "mrkdwn";
    private static final String PLAIN_TEXT = "plain_text";
    private static final String EMOJI = "emoji";
    private static final String HEADER = "header";
    private static final String SECTION = "section";

    private final String serviceName;
    private final String webHookUrl;

    private final RestOperations restClient;
    private final HttpHeaders headers;
    private final ObjectMapper objectMapper;

    /**
     * Constructor. Among other things, sets up retry event handlers for logging failed Slack API calls.
     *
     * @param serviceName  name of this service instance (for logging purposes - the web hook URL must not be revealed
     *                     because it contains a secret token)
     * @param webHookUrl   Slack webhook URL for sending messages
     * @param restClient   REST client
     * @param objectMapper JSON object mapper for constructing JSON objects sent to Slack
     */
    @ConstraintArguments
    public LightweightSlackServiceImpl(String serviceName, URL webHookUrl,
            RestOperations restClient, ObjectMapper objectMapper) {
        this.serviceName = checkNotBlank(serviceName, _LightweightSlackServiceImplArgumentsMeta.SERVICENAME.name());

        this.webHookUrl = check(webHookUrl, _LightweightSlackServiceImplArgumentsMeta.WEBHOOKURL.name())
                .notNull()
                .constraint(url -> !url.toString().contains("$"), "contains unexpanded environment variable?")
                .getValue()
                .toString();

        this.restClient = checkNotNull(restClient, _LightweightSlackServiceImplArgumentsMeta.RESTCLIENT.name());
        this.objectMapper = checkNotNull(objectMapper, _LightweightSlackServiceImplArgumentsMeta.OBJECTMAPPER.name());

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    }

    @ConstraintArguments
    @Override
    public void send(Map<String, ?> map) {
        checkNotNull(map, _LightweightSlackServiceImplSendArgumentsMeta.MAP.name());

        String json;
        try {
            json = objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error serializing to JSON: " + e.getMessage(), e);
        }

        if (log.isTraceEnabled()) {
            log.trace("Sending to Slack from '{}': {}", serviceName, json);
        } else {
            log.info("Sending to Slack  from '{}' (log level 'trace' additionally logs sensitive message content)",
                    serviceName);
        }

        var request = new HttpEntity<>(json, headers);
        callSlackApi(request);
    }

    @Override
    public void sendPlainText(String text) {
        send(Map.of(TEXT, text));
    }

    @Override
    public void sendMarkdown(String title, String text) {
        send(createMarkdownMessage(title, text));
    }

    private void callSlackApi(HttpEntity<?> request) {
        restClient.postForEntity(webHookUrl, request, Void.class);
    }

    private Map<String, Object> createMarkdownMessage(String title, String text) {
        // WARNING: Map.of() forbids null keys or values
        return Map.of(BLOCKS, List.of(
                createHeaderBlock(title),
                createSectionBlock(text)
        ));
    }

    private Map<String, Object> createHeaderBlock(String title) {
        return Map.of(
                TYPE, HEADER,
                TEXT, Map.of(
                        TYPE, PLAIN_TEXT,
                        TEXT, title,
                        EMOJI, true));
    }

    private Map<String, Object> createSectionBlock(String text) {
        return Map.of(
                TYPE, SECTION,
                TEXT, Map.of(
                        TYPE, MRKDWN,
                        TEXT, StringUtils.substring(text, 0, MAX_TEXT_LENGTH)));
    }

}
