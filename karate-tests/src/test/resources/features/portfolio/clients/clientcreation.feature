Feature: Test client apis
  Background:
    * callonce read('classpath:features/base.feature')
    * url baseUrl

  @ignore
  @createFetchUpdateEntityClient
  Scenario: Create fetch and update Entity client
    * def submittedOnDate = df.format(faker.date().past(30, 29, TimeUnit.DAYS))
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@createEntityStep') { clientCreationDate : '#(submittedOnDate)'}
    * def createdClientId = result.clientId

    # Activate client
    * def activatedClient = call read('classpath:features/portfolio/clients/clientsteps.feature@activateClientStep') { clientId : '#(createdClientId)'}
    * assert createdClientId == activatedClient.res.clientId

    # Fetch created client
    * def legalFormId = 2
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@findbyclientid') { clientId : '#(createdClientId)'}
    * def client = result.client
    * match createdClientId == client.id
    * match legalFormId == client.legalForm.id

    # Update fetched client
    * def accountNo = client.accountNo
    * def fullname = "Business update"
    * def updatedClient = call read('classpath:features/portfolio/clients/clientsteps.feature@updateEntityStep') { clientId : '#(createdClientId)', accountNo : '#(accountNo)'}
    * assert createdClientId == updatedClient.res.resourceId
    * match updatedClient.res.changes contains { externalId: '#notnull'}
    * assert fullname == updatedClient.res.changes.fullname

  @ignore
  @createFetchAndUpdatePersonClient
  Scenario: Create fetch and update Normal client
    * def submittedOnDate = df.format(faker.date().past(30, 29, TimeUnit.DAYS))
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@create') { clientCreationDate : '#(submittedOnDate)'}
    * def createdClientId = result.clientId

    # Fetch created client
    * def legalFormId = 1
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@findbyclientid') { clientId : '#(createdClientId)'}
    * def client = result.client
    * match createdClientId == client.id
    * match legalFormId == client.legalForm.id

    # Update fetched client
    * def updatedClient = call read('classpath:features/portfolio/clients/clientsteps.feature@update') { clientId : '#(createdClientId)'}
    * assert createdClientId == updatedClient.res.resourceId
    * match updatedClient.res.changes contains { externalId: '#notnull'}
    * assert fullname == updatedClient.res.changes.fullname

  @ignore
  #createClientWithSavings
  Scenario: Create client with savings account

    # Fetch saving product
    * def savingsProduct = call read('classpath:features/portfolio/products/savingsproduct.feature@fetchdefaultproduct')
    * def savingsProductId = savingsProduct.savingsProductId

    # Then create client
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@createClientWithSavingsStep') { savingsProductId : '#(savingsProductId)'}
    * def savingsId = result.client.savingsId

    # Fetch savings account for created client
    * def savingsResponse = call read('classpath:features/portfolio/savingsaccount/savingssteps.feature@findsavingsbyid') { savingsId : '#(savingsId)' }
    * assert savingsProductId == savingsResponse.savingsAccount.savingsProductId

  @createClientWithAddressEnable
  Scenario: Create client with Address Enable

    # Fetch config by name
    * def configName = 'Enable-Address'
    * def result = call read('classpath:features/administration/configurations/globalConfig.feature@fetchConfigStep') {name : '#(configName)'}
    * assert configName == result.configDetails.name
    * def config = result.configDetails

    * def enable = true
    * def updatedConfig = call read('classpath:features/administration/configurations/globalConfig.feature@checkAndEnableDisable') {configDetails : '#(config)', status : '#(enable)'}
    * def res = if(updatedConfig != null) karate.match("updatedConfig.res.changes contains {enabled : 'true'}")
    * print updatedConfig.res

    * def response = call read('classpath:features/administration/codes/codes.feature@fetchCodesStep')
    * assert karate.sizeOf(response.listOfCodes) > 0
    * def callback = function(x){ return x.name == 'ADDRESS_TYPE' }
    * def filteredCode = karate.filter(response.listOfCodes, callback)
    * print filteredCode[0]
    * assert filteredCode[0].name == 'ADDRESS_TYPE'
    * def codeId = filteredCode.name

    * def codeValuesRes = call read('classpath:features/administration/codes/codeValues.feature@fetchCodeValuesStep')
    * def res = if (codeValuesRes.listOfCodeValues.length == 0) karate.call('classpath:features/administration/codes/codeValues.feature@createCodeValueStep')
    * def addressTypeCvId = res.codeValue.id

    * def submittedOnDate = df.format(faker.date().past(30, 29, TimeUnit.DAYS))
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@createClientWithAddress') {clientCreationDate : '#(submittedOnDate)'}



