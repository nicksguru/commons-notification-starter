#@disabled
Feature: Email Service
  Email service should be able to send HTML emails with and without templates

  Scenario Outline: Send HTML email
    Given an email service is configured
    When an HTML email is sent from "<from>" to "<to>" with subject "<subject>" and body "<body>"
    Then the email should be sent
    Examples:
      | from           | to            | subject      | body           |
      | from@test.com  | to@test.com   | Test Subject | <p>Test</p>    |
      | other@test.com | user@test.com | Hello        | <h1>Hello</h1> |

  Scenario Outline: Send HTML email with template
    Given an email service is configured
    And a template "<template>" exists with context
      | key     | value           |
      | name    | John            |
      | message | Welcome aboard! |
    When an HTML email with template is sent from "<from>" to "<to>" with subject "<subject>" and template "<template>"
    Then the template should be rendered with correct name and parameters
    And the email should be sent
    Examples:
      | from           | to            | subject      | template         |
      | from@test.com  | to@test.com   | Test Subject | welcome.ftl      |
      | other@test.com | user@test.com | Hello        | notification.ftl |
