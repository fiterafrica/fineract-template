@ignore
Feature: Create loan stapes
  Background:
    * callonce read('classpath:features/base.feature')
    * url baseUrl
    * def productsData = read('classpath:templates/savings.json')


  #Set up parameters submittedOnDate, clientId, loanProductId, loanAmount, clientCreationDate
  @ignore
  @createloan
  Scenario: Create loan accounts
    Given configure ssl = true
    * def loanProduct = call read('classpath:features/portfolio/products/loanproduct.feature@fetchdefaultproduct')
    * def loanProductId = loanProduct.loanProductId
    #create savings account with clientCreationDate
    * def result = call read('classpath:features/portfolio/clients/clientsteps.feature@create') { clientCreationDate : '#(clientCreationDate)' }
    * def clientId = result.response.resourceId
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loan1
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId


  #ensure to set parameters approvalDate, loanAmount, loanId
  @ignore
  @approveloan
  Scenario: Approve loan accounts
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'approve'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.approve
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }

  #Ensure that we set loanAmount, disbursementDate, loanId
  @ignore
  @disburse
  Scenario: Disburse loans account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'disburse'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.disburse
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  #Ensure that we set repaymentDate, repaymentAmount, loanId
  @ignore
  @loanRepayment
  Scenario: Loan repayment
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId,'transactions'
    And params {command:'repayment'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.transaction
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId

  #ensure to set loanId
  @ignore
  @findloanbyid
  Scenario: Get loan account by id
    Given configure ssl = true
    Given path 'loans',loanId
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method GET
    Then status 200
    * def loanAccount = response

  @ignore
  @findloanbyidWithAllAssociationStep
  Scenario: Get loan account by id
    Given configure ssl = true
    Given path 'loans',loanId
    And params {associations:'all'}
    And params {exclude:'guarantors,futureSchedule'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method GET
    Then status 200
    * def loanAccount = response

  @ignore
  @createLoanWithSavingsAccountStep
  Scenario: Create loan accounts With Savings Account Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loanWithSavingsAccount
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @unDisburseLoanAccountStep
  Scenario: Un Disburse Loan Account Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'undodisbursal'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.unDisburseLoanAccountPayload
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @disburseToSavingsAccountStep
  Scenario: Disburse loan to Savings Account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'disbursetosavings'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.disburseToSavingsAccount
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @unApproveLoanAccountStep
  Scenario: Un Approve Loan Account Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'undoapproval'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.unApproveLoanAccountPayload
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @rejectedLoanAccountStep
  Scenario: Un Approve Loan Account Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'reject'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.rejectLoanApplicationPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @createloanTemplate400Step
  Scenario: Create loan accounts Template Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loan1
    When method POST
    Then status 400
    Then match $ contains { developerMessage: '#notnull' }

  @ignore
  @createloanTemplate403Step
  Scenario: Create loan accounts Template Step
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loan1
    When method POST
    Then match $ contains { developerMessage: '#notnull' }

    # This steps has no HardCodes Product
  @ignore
  @createLoanWithConfigurableProductStep
  Scenario: Create loan account With Configurable Product
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loanAccountWithNewProductPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @loanRepaymentSteps
  Scenario: Loan repayment Steps
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId,'transactions'
    And params {command:'repayment'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.transaction
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId


  # This steps create Loan Account with Disburse to savings Charge and Penalty Charge on a loan account
  @ignore
  @createLoanAccountWithDisburseToSavingsAccountChargeAndPenaltyChargeSteps
  Scenario: Create loan account With Configurable Product with Penalty Charge and Disburse to Savings Account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loanAccountWithNewProductWithDisburseToSavingsAccountAndPenaltyChargePayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

      # This steps create Loan Account with Disburse to savings Charge and Penalty Charge on a loan account
  @ignore
  @waiveLoanAccountChargesAndFeesSteps
  Scenario: Waive Loan Account Overdue Charges and Fees
    Given configure ssl = true
    * def chargesData = read('classpath:templates/loansCharges.json')
    Given path 'loans',loanId,'charges',chargeId
    And params {command:'waive'}
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request chargesData.waiveLoanAccountCharges
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

    #Run Clone Job to apply penalties
  @ignore
  @runCloneJobForLoanPenalty
  Scenario:
    Given configure ssl = true
    Given path 'loans',loanId
    And params {command:'runCloneJobForLoanPenalty'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

        #Run Clone Job to apply penalties
  @ignore
  @accountTransferFromSavingsAccountToLoanAccountSteps
  Scenario:
    Given configure ssl = true
    * def transferAccountData = read('classpath:templates/loans.json')
    Given path 'accounttransfers'
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request transferAccountData.transferFundsFromSavingsAccountToLoanAccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

   #Waive Interest on Loan Account
  @ignore
  @waiveInterestOnLoanAccountSteps
  Scenario: Waive Interest on Loan Account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId,'transactions'
    And params {command:'waiveinterest'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.waiveInterestOnLoanAccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId

   # WriteOff
  @ignore
  @writeOffOnLoanAccountSteps
  Scenario: WriteOff on Loan Account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId,'transactions'
    And params {command:'writeoff'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.writeOffOnLoanAccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId

   # Recovery Payment
  @ignore
  @recoveryPaymentLoanAccountSteps
  Scenario: recovery Payment on Loan Account
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId,'transactions'
    And params {command:'recoverypayment'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.recoveryPaymentOnLoanAccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId

  @ignore
  @loanRescheduleSteps
  Scenario: Loan reschedule Steps
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'rescheduleloans'
    And params {command:'reschedule'}
    And request loansData.loanReschedulePayLoad
    When method POST
    Then status 200
    Then match $ contains { loanId: '#notnull' }
    Then def loanId = response.loanId
    
  @ignore
  @OXY163loanScheduleWithInterestRecalculationEnabledIsOnly3PeriodsLongRegardlessOfTheNumberOfRepaymentsSetSteps
  Scenario: OXY-163 Loan Schedule with interest recalculation enabled is only 3 periods long regardless of the number of repayments set.
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.OXY163loanaccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @400-OXY163loanScheduleWithInterestRecalculationEnabledIsOnly3PeriodsLongRegardlessOfTheNumberOfRepaymentsSetSteps
  Scenario: OXY-163 Loan Schedule with interest recalculation enabled is only 3 periods long regardless of the number of repayments set.
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.OXY163loanaccountPayLoad
    When method POST
    Then status 400
    Then match $ contains { developerMessage: '#notnull' }

  @ignore
  @Update-400-OXY163loanScheduleWithInterestRecalculationEnabledIsOnly3PeriodsLongRegardlessOfTheNumberOfRepaymentsSetSteps
  Scenario: Update OXY-163 Loan Schedule with interest recalculation enabled is only 3 periods long regardless of the number of repayments set.
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.OXY163loanaccountPayLoad
    When method PUT
    Then status 400
    Then match $ contains { developerMessage: '#notnull' }

  @ignore
  @approveLoanShouldFailWhenDecisionEngineIsActivatedAndWorkFlowIsViolated
  Scenario: Approve loan accounts Should fail when decision engine is activated and workflow is violated
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'approve'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.approve
    When method POST
    Then status 403
    Then match $ contains { developerMessage: '#notnull' }
    Then assert response.developerMessage == 'Request was understood but caused a domain rule violation.'
    Then assert response.errors[0].developerMessage == 'Loan Account is not permitted for Approval since new workflow [Add-More-Stages-To-A-Loan-Life-Cycle] is activated and next status is [Review Application]'

  @ignore
  @disburseLoanShouldFailWhenDecisionEngineIsActivatedAndWorkFlowIsViolated
  Scenario: Disburse loans account Should fail when decision engine is activated and workflow is violated
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans',loanId
    And params {command:'disburse'}
    And header Accept = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.disburse
    When method POST
    Then status 403
    Then match $ contains { developerMessage: '#notnull' }
    Then assert response.developerMessage == 'Request was understood but caused a domain rule violation.'
    Then assert response.errors[0].developerMessage == 'Loan Account is not permitted for Disbursement since new workflow [Add-More-Stages-To-A-Loan-Life-Cycle] is activated and next stage is [Review Application]'


  @ignore
  @findLoanAccountNotesByLoanId
  Scenario: Get loan account Notes by Loan ID
    Given configure ssl = true
    Given path 'loans',loanId, 'notes'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    When method GET
    Then status 200
    Then def notes = response

  @ignore
  @createLoanWithConfigurableProductAndLoanTermStep
  Scenario: Create loan account With Configurable Product and Loan Term
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.loanAccountWithNewProductAndNumberOfRepaymentPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @createGroupLoanWithConfigurableProductAndLoanTermStep
  Scenario: Create Group loan account With Configurable Product and Loan Term
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.groupLoanAccountWithNewProductAndNumberOfRepaymentPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @createJLGLoanWithConfigurableProductAndLoanTermStep
  Scenario: Create JLG loan account With Configurable Product and Loan Term
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'loans'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.jlgLoanAccountWithNewProductAndNumberOfRepaymentPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId

  @ignore
  @createGLIMLoanWithConfigurableProductAndLoanTermStep
  Scenario: Create GLIM loan account With Configurable Product and Loan Term
    Given configure ssl = true
    * def loansData = read('classpath:templates/loans.json')
    Given path 'batches?enclosingTransaction=true'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And header Authorization = authToken
    And header fineract-platform-tenantid = tenantId
    And request loansData.createGLIMAccountPayLoad
    When method POST
    Then status 200
    Then match $ contains { resourceId: '#notnull' }
    Then def loanId = response.resourceId