import uuid


def product(name, code):
    return {"currencyCode": "USD",
            "digitsAfterDecimal": 2,
            "interestCompoundingPeriodType": 1,
            "interestPostingPeriodType": 4,
            "interestCalculationType": 1,
            "interestCalculationDaysInYearType": 365,
            "accountingRule": "1",
            "name": name,
            "shortName": code,
            "inMultiplesOf": "1",
            "nominalAnnualInterestRate": 0.1,
            "paymentChannelToFundSourceMappings": [],
            "feeToIncomeAccountMappings": [],
            "penaltyToIncomeAccountMappings": [],
            "charges": [],
            "locale": "en"}


def savings_account(product_id, client_id, external_id):
    return {"productId": product_id,
            "nominalAnnualInterestRate": 0.1,
            "withdrawalFeeForTransfers": False,
            "allowOverdraft": False,
            "enforceMinRequiredBalance": False,
            "withHoldTax": False,
            "interestCompoundingPeriodType": 1,
            "interestPostingPeriodType": 4,
            "interestCalculationType": 1,
            "interestCalculationDaysInYearType": 365,
            "externalId": external_id,
            "submittedOnDate": "25 January 2022",
            "locale": "en",
            "dateFormat": "dd MMMM yyyy",
            "monthDayFormat": "dd MMM",
            "charges": [],
            "clientId": client_id}


def transaction(amount, date):
    return {"transactionDate": date,
            "transactionAmount": amount,
            "note": str(uuid.uuid4()),
            "locale": "en",
            "dateFormat": "dd MMMM yyyy"}


def deposit_transaction_search(clientId):
    return [
        {
            "filterSelection": "TRANSACTION_AMOUNT",
            "filterElement": "NOT_EMPTY"
        },
        {
            "filterSelection": "ACCOUNT_OWNER_ID",
            "filterElement": "EQUALS",
            "value": clientId
        },
        {
            "filterSelection": "TRANSACTION_TYPE",
            "filterElement": "IN",
            "values": [1,2]
        }
    ]

def client_search():
    return [
        {
        "filterElement": "EQUALS",
        "filterSelection": "FIRST_NAME",
        "value": "FELICIA"
        },
        {
            "filterElement": "EQUALS",
            "filterSelection": "LAST_NAME",
            "value": "ABEJIDE"
        },
        {
            "filterElement": "EQUALS",
            "filterSelection": "DATE_OF_BIRTH",
            "value": "2001-06-19"
        },
        {
            "filterElement": "EQUALS",
            "filterSelection": "MOBILE_NUMBER",
            "value": "+2348028348502"
        }
    ]
