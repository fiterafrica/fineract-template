Feature: Currency creation api tests
  Background:
    * callonce read('classpath:features/base.feature')
    * url baseUrl
    * configure ssl = true

  @ignore
  @fetchCodesStep
  Scenario: Fetch Codes
    Given path 'codes'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method GET
    Then status 200
    Then def listOfCodes = response
