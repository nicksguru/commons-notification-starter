package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.notification.NotificationCategory;
import guru.nicks.commons.notification.NotificationTransport;
import guru.nicks.commons.notification.impl.NotificationServiceImpl;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.togglz.core.Feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Step definitions for testing {@link NotificationServiceImpl}.
 */
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceSteps {

    // DI
    private final TextWorld textWorld;

    private final List<TestNotificationTransport> transports = new ArrayList<>();
    private final TestNotificationTransport transport1 = new TestNotificationTransport("Transport1");
    private final TestNotificationTransport transport2 = new TestNotificationTransport("Transport2");
    private final TestNotificationTransport transport3 = new TestNotificationTransport("Transport3");

    private NotificationServiceImpl<TestCategory> notificationService;
    private TestCategory category;
    private String message;
    private Map<String, Object> messageContext = new HashMap<>();

    private Boolean sendResult;
    private boolean featureEnabled = true;
    private Logger fallbackLogger;
    private BiConsumer<String, Throwable> errorNotifier;

    @DataTableType
    public MessageContext createMessageContext(Map<String, String> entry) {
        return MessageContext.builder()
                .key(StringUtils.stripToNull(entry.get("key")))
                .value(StringUtils.stripToNull(entry.get("value")))
                .build();
    }

    @When("notification service is created with empty transports list")
    public void notificationServiceIsCreatedWithEmptyTransportsList() {
        var throwable = catchThrowable(() ->
                new NotificationServiceImpl<>(List.of(), feature -> featureEnabled));
        textWorld.setLastException(throwable);
    }

    @When("notification service is created with null transports list")
    public void notificationServiceIsCreatedWithNullTransportsList() {
        var throwable = catchThrowable(() ->
                new NotificationServiceImpl<>(null, feature -> featureEnabled));
        textWorld.setLastException(throwable);
    }

    @Given("a notification service is configured with {int} transport(s)")
    public void notificationServiceIsConfiguredWithTransport(int count) {
        for (int i = 0; i < count; i++) {
            var transport = switch (i) {
                case 0 -> transport1;
                case 1 -> transport2;
                case 2 -> transport3;
                default -> throw new IllegalArgumentException("Unsupported transport index: " + i);
            };

            transports.add(transport);
        }

        notificationService = new NotificationServiceImpl<>(transports, feature -> featureEnabled);
    }

    @Given("a notification service is configured with duplicate transports")
    public void notificationServiceIsConfiguredWithDuplicateTransports() {
        transports.add(transport1);
        transports.add(transport1);
        transports.add(transport2);

        notificationService = new NotificationServiceImpl<>(transports, feature -> featureEnabled);
    }

    @Given("transport {int} fails with exception {string}")
    public void transportFailsWithException(int transportNumber, String exceptionClassName) {
        var transport = getTransport(transportNumber);
        RuntimeException exception = createException(exceptionClassName);
        transport.setExceptionToThrow(exception);
    }

    @When("notification is sent with category {string} message {string} and empty context")
    public void notificationIsSentWithCategoryMessageAndEmptyContext(String category, String message) {
        this.category = TestCategory.valueOf(category.toUpperCase());
        this.message = message;
        this.messageContext = Map.of();

        sendResult = notificationService.send(this.category, this.message, this.messageContext);
    }

    @When("notification is sent with category {string} message {string} and null context")
    public void notificationIsSentWithCategoryMessageAndNullContext(String category, String message) {
        this.category = TestCategory.valueOf(category.toUpperCase());
        this.message = message;
        this.messageContext = null;

        sendResult = notificationService.send(this.category, this.message, this.messageContext);
    }

    @When("notification is sent with category {string} message {string} and context")
    public void notificationIsSentWithCategoryMessageAndContext(String category, String message,
            List<MessageContext> contexts) {
        this.category = TestCategory.valueOf(category.toUpperCase());
        this.message = message;
        this.messageContext = new HashMap<>();

        for (var context : contexts) {
            if (context.key() != null) {
                this.messageContext.put(context.key(), context.value());
            }
        }

        sendResult = notificationService.send(this.category, this.message, this.messageContext);
    }

    @Then("the notification should be sent successfully")
    public void notificationShouldBeSentSuccessfully() {
        assertThat(sendResult)
                .as("sendResult")
                .isTrue();
    }

    @Then("the notification should not be sent successfully")
    public void notificationShouldNotBeSentSuccessfully() {
        assertThat(sendResult)
                .as("sendResult")
                .isFalse();
    }

    @Then("transport {int} should be called exactly {int} time(s)")
    public void transportShouldBeCalledExactlyTimes(int transportNumber, int times) {
        var transport = getTransport(transportNumber);

        assertThat(transport.getCallCount())
                .as("transport " + transportNumber + " call count")
                .isEqualTo(times);
    }

    @Then("all {int} transports should be called")
    public void allTransportsShouldBeCalled(int count) {
        assertThat(transports)
                .as("transports")
                .hasSize(count);

        for (var transport : transports) {
            assertThat(transport.getCallCount())
                    .as("transport call count")
                    .isGreaterThan(0);
        }
    }

    @Then("transport {int} should be called with category {string} message {string} and context size {int}")
    public void transportShouldBeCalledWithCategoryMessageAndContextSize(int transportNumber,
            String expectedCategory, String expectedMessage, int expectedContextSize) {
        var transport = getTransport(transportNumber);
        var expectedCategoryObj = TestCategory.valueOf(expectedCategory.toUpperCase());

        assertThat(transport.getCallCount())
                .as("transport " + transportNumber + " call count")
                .isGreaterThan(0);

        assertThat(transport.getLastCategory())
                .as("last category")
                .isEqualTo(expectedCategoryObj);

        assertThat(transport.getLastMessage())
                .as("last message")
                .isEqualTo(expectedMessage);

        assertThat(transport.getLastMessageContext())
                .as("last message context size")
                .hasSize(expectedContextSize);
    }

    @Given("the feature is enabled")
    public void featureIsEnabled() {
        featureEnabled = true;
    }

    @Given("the feature is disabled")
    public void featureIsDisabled() {
        featureEnabled = false;
    }

    @When("an error notifier is created for category {string} with a fallback logger")
    public void errorNotifierIsCreatedForCategoryWithFallbackLogger(String category) {
        var testCategory = TestCategory.valueOf(category.toUpperCase());
        fallbackLogger = mock(Logger.class);
        errorNotifier = notificationService.wrapErrorNotifier(TestFeature.NOTIFICATIONS, testCategory, fallbackLogger);
    }

    @When("the error notifier is called with message {string} and a {string}")
    public void errorNotifierIsCalledWithMessageAndException(String message, String exceptionClassName) {
        var exception = createException(exceptionClassName);
        errorNotifier.accept(message, exception);
    }

    @Then("the fallback logger should receive the message {string}")
    public void fallbackLoggerShouldReceiveMessage(String expectedMessage) {
        verify(fallbackLogger).error(eq(expectedMessage), any(Throwable.class));
    }

    @Then("transport {int} should be called with message containing {string}")
    public void transportShouldBeCalledWithMessageContaining(int transportNumber, String expectedFragment) {
        var transport = getTransport(transportNumber);
        assertThat(transport.getLastMessage())
                .as("last message")
                .contains(expectedFragment);
    }

    private TestNotificationTransport getTransport(int transportNumber) {
        return transports.get(transportNumber - 1);
    }

    private RuntimeException createException(String className) {
        return switch (className) {
            case "RuntimeException" -> new RuntimeException("Transport failed");
            case "IllegalStateException" -> new IllegalStateException("Illegal state");
            case "NullPointerException" -> new NullPointerException("Null value");
            default -> new RuntimeException("Unknown exception: " + className);
        };
    }

    /**
     * Test implementation of {@link NotificationCategory}.
     */
    @Getter
    public enum TestCategory implements NotificationCategory {

        INFO(Level.INFO, "Information"),
        WARNING(Level.WARN, "Warning"),
        ERROR(Level.ERROR, "Error");

        private final Level logLevel;
        private final String description;

        TestCategory(Level logLevel, String description) {
            this.logLevel = logLevel;
            this.description = description;
        }
    }

    /**
     * Test feature for toggle testing.
     */
    public enum TestFeature implements Feature {
        NOTIFICATIONS
    }

    /**
     * Test implementation of {@link NotificationTransport}.
     */
    public static class TestNotificationTransport implements NotificationTransport<TestCategory> {

        private final String name;
        private final AtomicInteger callCount = new AtomicInteger(0);

        @Setter
        private RuntimeException exceptionToThrow;
        @Getter
        private TestCategory lastCategory;

        @Getter
        private String lastMessage;
        @Getter
        private Map<String, ?> lastMessageContext;

        public TestNotificationTransport(String name) {
            this.name = name;
        }

        @Override
        public void send(@Nonnull TestCategory category, @Nonnull String message,
                @Nonnull Map<String, ?> messageContext) {
            callCount.incrementAndGet();

            this.lastCategory = category;
            this.lastMessage = message;
            this.lastMessageContext = messageContext;

            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }

        public int getCallCount() {
            return callCount.get();
        }

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * Data table class for message context.
     */
    @Builder(toBuilder = true)
    public record MessageContext(

            String key,
            String value) {

    }

}
