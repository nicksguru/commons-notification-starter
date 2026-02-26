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
    And transport 1 sends successfully
    When notification is sent with category "INFO" message "Test message" and context
      | key  | value |
      | user | test  |
    Then the notification should be sent successfully

  Scenario: Send notification via multiple successful transports
    Given a notification service is configured with 3 transports
    And all transports send successfully
    When notification is sent with category "ERROR" message "Error occurred" and empty context
    Then the notification should be sent successfully

  Scenario Outline: Send notification when some transports fail
    Given a notification service is configured with 3 transports
    And transport 1 sends successfully
    And transport 2 fails with exception "<exception2>"
    And transport 3 sends successfully
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
    And transport 1 sends successfully
    When notification is sent with category "INFO" message "Test message" and null context
    Then the notification should be sent successfully

  Scenario Outline: Send notification with different categories
    Given a notification service is configured with 1 transport
    And transport 1 sends successfully
    When notification is sent with category "<category>" message "Category test" and empty context
    Then the notification should be sent successfully
    Examples:
      | category |
      | INFO     |
      | WARNING  |
      | ERROR    |

  Scenario: Transport is called with correct parameters
    Given a notification service is configured with 1 transport
    And transport 1 sends successfully
    When notification is sent with category "INFO" message "Test message" and context
      | key       | value   |
      | userId    | 123     |
      | requestId | abc-456 |
    Then the notification should be sent successfully
    And transport 1 should be called with category "INFO" message "Test message" and context size 2

  Scenario: Send notification without context map
    Given a notification service is configured with 1 transport
    And transport 1 sends successfully
    When notification is sent with category "INFO" message "Simple message" and empty context
    Then the notification should be sent successfully
    And transport 1 should be called with category "INFO" message "Simple message" and context size 0

  Scenario: Parallel execution of multiple transports
    Given a notification service is configured with 3 transports
    And all transports send successfully
    When notification is sent with category "INFO" message "Parallel test" and empty context
    Then the notification should be sent successfully
    And all 3 transports should be called
