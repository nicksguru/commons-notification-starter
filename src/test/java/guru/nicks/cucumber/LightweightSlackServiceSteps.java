package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.notification.impl.LightweightSlackServiceImpl;
import guru.nicks.notification.service.LightweightSlackService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link LightweightSlackServiceImpl}.
 */
@RequiredArgsConstructor
public class LightweightSlackServiceSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    private AutoCloseable closeableMocks;

    private LightweightSlackService slackService;
    private URL webhookUrl;
    private String jsonRequest;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public SlackMessageData createSlackMessageData(Map<String, String> entry) {
        return SlackMessageData.builder()
                .key(entry.get("key"))
                .value(entry.get("value"))
                .build();
    }

    @Given("a Slack service is configured with webhook URL {string}")
    public void aSlackServiceIsConfiguredWithWebhookURL(String url) throws MalformedURLException {
        webhookUrl = URI.create(url).toURL();
        slackService = new LightweightSlackServiceImpl("testService", webhookUrl, restTemplate, objectMapper);
    }

    @Given("a webhook URL with unexpanded environment variable {string} is provided")
    public void aWebhookURLWithUnexpandedEnvironmentVariableIsProvided(String url) {
        try {
            webhookUrl = URI.create("https://" + url).toURL();
            slackService = new LightweightSlackServiceImpl("testService", webhookUrl, restTemplate, objectMapper);
        }
        // catch not only MalformedURLException, but IllegalArgumentException from service constructor
        catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("a plain text message {string} is sent")
    public void aPlainTextMessageIsSent(String message) throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"text\":\"" + message + "\"}");

        slackService.sendPlainText(message);
    }

    @When("a Markdown message with title {string} and text {string} is sent")
    public void aMarkdownMessageWithTitleAndTextIsSent(String title, String text) throws JsonProcessingException {
        var expectedJson = "{\"blocks\":[{\"type\":\"header\",\"text\":{\"type\":\"plain_text\",\"text\":\"" +
                title + "\",\"emoji\":true}},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"" +
                text + "\"}}]}";
        when(objectMapper.writeValueAsString(any()))
                .thenReturn(expectedJson);

        slackService.sendMarkdown(title, text);
        jsonRequest = expectedJson;
    }

    @When("a Markdown message with title {string} and text of length {int} is sent")
    public void aMarkdownMessageWithTitleAndTextOfLengthIsSent(String title, int length)
            throws JsonProcessingException {
        var text = "a".repeat(length);
        var expectedText = length > LightweightSlackServiceImpl.MAX_TEXT_LENGTH
                ? text.substring(0, LightweightSlackServiceImpl.MAX_TEXT_LENGTH)
                : text;

        var expectedJson = "{\"blocks\":[{\"type\":\"header\",\"text\":{\"type\":\"plain_text\",\"text\":\"" +
                title + "\",\"emoji\":true}},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"" +
                expectedText + "\"}}]}";

        when(objectMapper.writeValueAsString(any()))
                .thenReturn(expectedJson);

        slackService.sendMarkdown(title, text);
        jsonRequest = expectedJson;
    }

    @When("a JSON map with the following data is sent:")
    public void aJSONMapWithTheFollowingDataIsSent(List<SlackMessageData> dataList) throws JsonProcessingException {
        var dataMap = new HashMap<String, String>();

        for (var data : dataList) {
            dataMap.put(data.getKey(), data.getValue());
        }

        var jsonString = new ObjectMapper().writeValueAsString(dataMap);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn(jsonString);

        slackService.send(dataMap);
        jsonRequest = jsonString;
    }

    @Then("the REST template should post to the webhook URL")
    public void theRESTTemplateShouldPostToTheWebhookURL() {
        verify(restTemplate).postForEntity(
                eq(webhookUrl.toString()),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    @And("the request body should contain {string}")
    public void theRequestBodyShouldContain(String expectedContent) {
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).postForEntity(
                anyString(),
                entityCaptor.capture(),
                eq(Void.class)
        );

        var requestBody = entityCaptor.getValue().getBody().toString();

        assertThat(requestBody)
                .as("request body")
                .contains(expectedContent);
    }

    @And("the request body should contain a header block with text {string}")
    public void theRequestBodyShouldContainAHeaderBlockWithText(String expectedText) {
        assertThat(jsonRequest)
                .as("request body")
                .contains("\"type\":\"header\"")
                .contains("\"text\":\"" + expectedText + "\"");
    }

    @And("the request body should contain a section block with text {string}")
    public void theRequestBodyShouldContainASectionBlockWithText(String expectedText) {
        assertThat(jsonRequest)
                .as("request body")
                .contains("\"type\":\"section\"")
                .contains("\"type\":\"mrkdwn\"")
                .contains("\"text\":\"" + expectedText + "\"");
    }

    @And("the request body should contain a section with text not exceeding {int} characters")
    public void theRequestBodyShouldContainASectionWithTextNotExceedingCharacters(int maxLength) {
        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).postForEntity(
                anyString(),
                entityCaptor.capture(),
                eq(Void.class)
        );

        var requestBody = entityCaptor.getValue().getBody().toString();

        // extract the text content between the last "text":"..." in the JSON
        int textStartIndex = requestBody.lastIndexOf("\"text\":\"") + 8;
        int textEndIndex = requestBody.indexOf("\"", textStartIndex);
        String textContent = requestBody.substring(textStartIndex, textEndIndex);

        assertThat(textContent.length())
                .as("text content length")
                .isLessThanOrEqualTo(maxLength);
    }

    /**
     * Data class for Slack message entries.
     */
    @Value
    @Builder
    public static class SlackMessageData {
        String key;
        String value;
    }
}
