Feature: Configurations api tests
  Background:
    * callonce read('classpath:features/base.feature')
    * url baseUrl
    * configure ssl = true
    * def administration = read('classpath:templates/administration.json')

    * def enableDisableWithCheck =
        """
          function(data) {
              if (data.configDetails.enabled != data.status) {
                var updatedConfig = karate.call('classpath:features/administration/configurations/globalConfig.feature@enableDisableConfigStep', {configId : data.configDetails.id});
                return updatedConfig;
              }else{
                return '';
              }
          }
        """

  @ignore
  @fetchConfigStep
  Scenario: Fetch Global Configuration By name
    Given path 'configurations/name', name
    Given configure ssl = true
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method GET
    Then status 200
    Then def configDetails = response

  @ignore
  @enableDisableConfigStep
  Scenario: Enable/Disable Global Config
    Given configure ssl = true
    Given path 'configurations' ,configId
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request administration.enableDisableConfigPayload
    When method PUT
    Then status 200
    Then match $ contains {changes : '#notnull'}
    Then def config = response


  @ignore
  @checkAndEnableDisable
  Scenario: Check and update configuration
    * def data = {configDetails : '#(configDetails)', status : '#(status)'}
    * def updatedConfig = call enableDisableWithCheck data
    * def res = updatedConfig.response

