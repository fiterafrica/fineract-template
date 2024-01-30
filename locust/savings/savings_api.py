from pprint import pprint

import requests

import common.helpers as helpers
from accounting.accounting_api import create_gl_account, AccountType
from common.config import FINERACT_HEADERS, FINERACT_FULL_BE_API
from payload import savings_payload

PRODUCT_API = FINERACT_FULL_BE_API + '/savingsproducts'
ACCOUNT_API = FINERACT_FULL_BE_API + '/savingsaccounts'


def create_product(name, code):
    product_json = savings_payload.product(name, code)

    print("     ---> Creating product: " + name)

    response = requests.post(url=PRODUCT_API,
                             json=product_json,
                             headers=FINERACT_HEADERS)

    assert response.status_code == 200

    json = response.json()
    print("Product created successfully" + str(json))
    return json


def create_cash_product(name, code):
    saving_ref = create_gl_account(helpers.padWithRandomString("S_A_SavingsRef_", 20), AccountType.ASSET)
    overdraft_portfolio = create_gl_account(helpers.padWithRandomString("S_A_OD_Portfolio_", 20), AccountType.ASSET)

    saving_control = create_gl_account(helpers.padWithRandomString("S_L_Control_", 20), AccountType.LIABILITY)
    transfer_in_suspense = create_gl_account(helpers.padWithRandomString("S_L_Transfer", 20), AccountType.LIABILITY)

    interest_on_saving = create_gl_account(helpers.padWithRandomString("S_E_Interest_", 20), AccountType.EXPENSE)
    write_off = create_gl_account(helpers.padWithRandomString("S_E_WriteOff_", 20), AccountType.EXPENSE)

    income_from_fees = create_gl_account(helpers.padWithRandomString("S_I_Fees_", 20), AccountType.INCOME)
    income_form_penalties = create_gl_account(helpers.padWithRandomString("S_I_Penalties_", 20), AccountType.INCOME)
    income_from_overdraft = create_gl_account(helpers.padWithRandomString("S_I_Overdraft_", 20), AccountType.INCOME)

    product_json = savings_payload.product(name, code)

    product_json.update({
        "accountingRule": 2,

        "savingsReferenceAccountId": saving_ref,
        "overdraftPortfolioControlId": overdraft_portfolio,

        "savingsControlAccountId": saving_control,
        "transfersInSuspenseAccountId": transfer_in_suspense,

        "interestOnSavingsAccountId": interest_on_saving,
        "writeOffAccountId": write_off,

        "incomeFromFeeAccountId": income_from_fees,
        "incomeFromInterestId": income_from_overdraft,
        "incomeFromPenaltyAccountId": income_form_penalties
    })

    # pretty print map
    # pprint(product_json)

    print("     ---> Creating product: " + name)

    response = requests.post(url=PRODUCT_API,
                             json=product_json,
                             headers=FINERACT_HEADERS)

    helpers.raise_if_http_error(response)

    return response.json()


def list_products():
    response = requests.get(PRODUCT_API, headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def find_product(product_list, product_name):
    return next((p for p in product_list if p['name'] == product_name), None)


def create_and_return_product():
    product_name = "Saving Prod_" + helpers.timeStamp()
    product_code = helpers.randomString(4)

    response = create_cash_product(product_name, product_code)

    return {"id": response['resourceId']}

    # sleep and wait for the account to be created
    # helpers.sleep(2)

    # return find_product(list_products(), product_name)


def create_savings_account(product_id, client_id, external_id):
    json = savings_payload.savings_account(product_id, client_id, external_id)

    print("     ---> Creating savings account: " + external_id)

    response = requests.post(ACCOUNT_API, json=json, headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def list_savings_accounts(params):
    response = requests.get(url=ACCOUNT_API,
                            headers=FINERACT_HEADERS,
                            params=params)

    assert response.status_code == 200

    return response.json()


# 1007

def find_savings_account(account_list, external_id):
    return next((a for a in account_list['pageItems'] if a['externalId'] == external_id), None)


def activate_saving_account(savings_id):
    response = requests.post(f'{ACCOUNT_API}/{savings_id}?command=approve',
                             json={"locale": "en", "dateFormat": "dd MMMM yyyy", "approvedOnDate": "25 January 2022"},
                             headers=FINERACT_HEADERS)

    assert response.status_code == 200

    helpers.sleep(1)

    requests.post(f'{ACCOUNT_API}/{savings_id}?command=activate',
                  json={"locale": "en", "dateFormat": "dd MMMM yyyy", "activatedOnDate": "25 January 2022"},
                  headers=FINERACT_HEADERS)

    helpers.sleep(1)


def create_and_return_savings_account(product_id, client_id):
    external_id = "SAVING_" + helpers.timeStamp()

    response = create_savings_account(product_id,
                                      client_id,
                                      external_id)

    return {"id": response['resourceId']}

    # sleep and wait for the account to be created
    # helpers.sleep(2)
    #
    # return find_savings_account(list_savings_accounts({"externalId": external_id}), external_id)
