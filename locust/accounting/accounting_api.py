from enum import Enum

import requests

import common.helpers as helpers
from common.config import FINERACT_HEADERS, FINERACT_FULL_BE_API
from payload import savings_payload

ACCOUNTING_API = FINERACT_FULL_BE_API + '/glaccounts'


class AccountType(Enum):
    ASSET = 1
    LIABILITY = 2
    EQUITY = 3
    INCOME = 4
    EXPENSE = 5


def create_gl_account(name, account_type, gl_code=None):
    if gl_code is None:
        gl_code = helpers.randomString(10)

    print(" ---> Creating GL Account: " + name)

    payload = {
        "name": name,
        "type": account_type.value,
        "glCode": gl_code,
        "description": "XXX",
        "manualEntriesAllowed": True,
        "usage": "1"
    }

    response = requests.post(
        ACCOUNTING_API,
        json=payload,
        headers=FINERACT_HEADERS)

    helpers.raise_if_http_error(response)

    json = response.json()

    return json['resourceId']
