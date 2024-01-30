from pprint import pprint

import requests

import common.helpers as helpers
from common.config import FINERACT_HEADERS, FINERACT_FULL_BE_API
from payload import loan_payload

PRODUCT_API = FINERACT_FULL_BE_API + '/loanproducts'
ACCOUNT_API = FINERACT_FULL_BE_API + '/loans'


def find_loan_account(loan_account_list, external_id):
    # print params
    print("external_id: " + external_id)
    pprint(loan_account_list)
    return next((loan for loan in loan_account_list if loan['externalId'] == external_id), None)


def find_product(product_list, product_name):
    return next((p for p in product_list if p['name'] == product_name), None)


def list_products():
    response = requests.get(PRODUCT_API, headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def list_loan_accounts(params):
    response = requests.get(ACCOUNT_API,
                            params=params,
                            headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def create_loan(product_id, client_id, external_id):
    loan_json = loan_payload.loan_application(product_id, client_id, external_id)

    print("     ---> Creating loan: " + external_id)

    response = requests.post(url=ACCOUNT_API,
                             json=loan_json,
                             headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def create_product(name, code):
    product_json = loan_payload.product(name, code)

    print("     ---> Creating loan product: " + name)

    response = requests.post(url=PRODUCT_API,
                             json=product_json,
                             headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def create_and_return_product():
    product_name = "Loan Prod_" + helpers.timeStamp()
    product_code = helpers.randomString(4)

    create_product(product_name, product_code)

    return find_product(list_products(), product_name)


def create_and_return_loan_account(product_id, client_id):
    loan_account_external_id = "LoanAccount_" + helpers.timeStamp()

    create_loan(product_id, client_id, loan_account_external_id)

    while True:
        loan_account_list = list_loan_accounts({"externalId": loan_account_external_id})
        loan_account = find_loan_account(loan_account_list, loan_account_external_id)
        if loan_account is not None:
            return loan_account
