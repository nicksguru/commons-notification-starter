#@disabled
Feature: Lightweight Slack Service

  Scenario: Sending plain text message to Slack
    Given a Slack service is configured with webhook URL "https://hooks.slack.com/services/test"
    When a plain text message "Hello Slack!" is sent
    Then the REST template should post to the webhook URL
    And the request body should contain "Hello Slack!"

  Scenario: Sending Markdown message to Slack
    Given a Slack service is configured with webhook URL "https://hooks.slack.com/services/test"
    When a Markdown message with title "Alert" and text "**Important** message" is sent
    Then the REST template should post to the webhook URL
    And the request body should contain a header block with text "Alert"
    And the request body should contain a section block with text "**Important** message"

  Scenario Outline: Handling text length limits in Markdown messages
    Given a Slack service is configured with webhook URL "https://hooks.slack.com/services/test"
    When a Markdown message with title "Test" and text of length <textLength> is sent
    Then the REST template should post to the webhook URL
    And the request body should contain a section with text not exceeding 3000 characters
    Examples:
      | textLength |
      | 100        |
      | 3000       |
      | 5000       |

  Scenario: Sending JSON map to Slack
    Given a Slack service is configured with webhook URL "https://hooks.slack.com/services/test"
    When a JSON map with the following data is sent:
      | key      | value   |
      | text     | Hello   |
      | username | TestBot |
    Then the REST template should post to the webhook URL
    And the request body should contain "Hello"
    And the request body should contain "TestBot"

  Scenario: Handling invalid webhook URL
    Given a webhook URL with unexpanded environment variable "$SLACK_URL" is provided
    Then an exception should be thrown
