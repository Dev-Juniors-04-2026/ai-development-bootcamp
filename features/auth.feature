@auth
Feature: User authentication

  Background:
    Given I am on the login page

  Scenario: Unauthenticated user sees the login screen
    Then the login form is visible
    And the todo list is not visible

  Scenario: User registers with valid credentials
    When I register with username "alice" and password "secret123"
    Then the todo list is visible
    And the login form is not visible

  Scenario: User registers with a taken username
    Given a user with username "bob" is already registered
    When I register with username "bob" and password "anything"
    Then I see the auth error "Username already taken."

  Scenario: User logs in with correct credentials
    Given a user with username "carol" and password "pass456" is already registered
    When I log in with username "carol" and password "pass456"
    Then the todo list is visible
    And the login form is not visible

  Scenario: User logs in with wrong password
    Given a user with username "dave" and password "correct" is already registered
    When I log in with username "dave" and password "wrong"
    Then I see the auth error "Invalid username or password."

  Scenario: User logs out
    Given I am logged in as username "eve" with password "pass789"
    When I click log out
    Then the login form is visible
    And the todo list is not visible

  Scenario: Invalid stored token returns the user to the login page
    When the stored token is invalid and the app reloads
    Then the login form is visible
    And the todo list is not visible
    And I see the auth error "Your session expired. Please log in again."

  Scenario: Username field does not accept more than 50 characters
    When I type 51 characters into the username field
    Then the username field contains at most 50 characters

  Scenario: Password field does not accept more than 100 characters
    When I type 101 characters into the password field
    Then the password field contains at most 100 characters

  @regression
  Scenario: User registers with blank username
    When I register with username "" and password "secret"
    Then I see the auth error "username: must not be blank"

  @regression
  Scenario: User registers with blank password
    When I register with username "alice" and password ""
    Then I see the auth error "password: must not be blank"
