#@disabled
Feature: Notification Service
  Notification service should be able to send notifications via multiple transports and handle failures gracefully

  Scenario: Validate that empty transports list throws exception
    When notification service is created with empty transports list
    Then IllegalArgumentException should be thrown
    And the exception message should contain "No notification transports defined"

  Scenario: Validate that null transports list throws exception
    When notification service is created with null transports list
    Then an exception should be thrown
    And the exception message should contain "No notification transports defined"

  Scenario: Send notification via single successful transport
    Given a notification service is configured with 1 transport
    When notification is sent with category "INFO" message "Test message" and context
      | key  | value |
      | user | test  |
    Then the notification should be sent successfully

  Scenario: Send notification via multiple successful transports
    Given a notification service is configured with 3 transports
    When notification is sent with category "ERROR" message "Error occurred" and empty context
    Then the notification should be sent successfully

  Scenario Outline: Send notification when some transports fail
    Given a notification service is configured with 3 transports
    And transport 2 fails with exception "<exception2>"
    When notification is sent with category "WARNING" message "Warning message" and empty context
    Then the notification should be sent successfully
    Examples:
      | exception2            |
      | RuntimeException      |
      | IllegalStateException |
      | NullPointerException  |

  Scenario Outline: Send notification when all transports fail
    Given a notification service is configured with 2 transports
    And transport 1 fails with exception "<exception1>"
    And transport 2 fails with exception "<exception2>"
    When notification is sent with category "ERROR" message "Critical error" and empty context
    Then the notification should not be sent successfully
    Examples:
      | exception1            | exception2            |
      | RuntimeException      | IllegalStateException |
      | NullPointerException  | RuntimeException      |
      | IllegalStateException | NullPointerException  |

  Scenario: Duplicate transports are removed during construction
    Given a notification service is configured with duplicate transports
    When notification is sent with category "INFO" message "Test" and empty context
    Then the notification should be sent successfully
    And transport 1 should be called exactly 1 time

  Scenario: Send notification with null message context
    Given a notification service is configured with 1 transport
    When notification is sent with category "INFO" message "Test message" and null context
    Then the notification should be sent successfully

  Scenario Outline: Send notification with different categories
    Given a notification service is configured with 1 transport
    When notification is sent with category "<category>" message "Category test" and empty context
    Then the notification should be sent successfully
    Examples:
      | category |
      | INFO     |
      | WARNING  |
      | ERROR    |

  Scenario: Transport is called with correct parameters
    Given a notification service is configured with 1 transport
    When notification is sent with category "INFO" message "Test message" and context
      | key       | value   |
      | userId    | 123     |
      | requestId | abc-456 |
    Then the notification should be sent successfully
    And transport 1 should be called with category "INFO" message "Test message" and context size 2

  Scenario: Send notification without context map
    Given a notification service is configured with 1 transport
    When notification is sent with category "INFO" message "Simple message" and empty context
    Then the notification should be sent successfully
    And transport 1 should be called with category "INFO" message "Simple message" and context size 0

  Scenario: Parallel execution of multiple transports
    Given a notification service is configured with 3 transports
    When notification is sent with category "INFO" message "Parallel test" and empty context
    Then the notification should be sent successfully
    And all 3 transports should be called

  Scenario: wrapNotifier sends notification when feature is enabled
    Given a notification service is configured with 1 transport
    And the feature is enabled
    When an error notifier is created for category "INFO" with a fallback logger
    And the error notifier is called with message "Something failed" and a "RuntimeException"
    Then transport 1 should be called exactly 1 time
    And transport 1 should be called with message containing "Something failed"

  Scenario: wrapNotifier logs to fallback logger when feature is disabled
    Given a notification service is configured with 1 transport
    And the feature is disabled
    When an error notifier is created for category "ERROR" with a fallback logger
    And the error notifier is called with message "Boot failed" and a "IllegalStateException"
    Then the fallback logger should receive the message "Boot failed"
    And transport 1 should be called exactly 0 times

  Scenario Outline: wrapNotifier respects feature toggle for different categories
    Given a notification service is configured with 1 transport
    And the feature is <featureState>
    When an error notifier is created for category "<category>" with a fallback logger
    And the error notifier is called with message "Test message" and a "RuntimeException"
    Then transport 1 should be called exactly <transportCalls> time
    Examples:
      | category | featureState | transportCalls |
      | INFO     | enabled      | 1              |
      | WARNING  | enabled      | 1              |
      | ERROR    | enabled      | 1              |
      | INFO     | disabled     | 0              |
      | ERROR    | disabled     | 0              |
