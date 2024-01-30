import random

from locust import HttpUser, task, events, between

import client.client_api as capi
import savings.savings_api as spi
from common import helpers
from common.config import (FINERACT_FULL_API, FINERACT_HEADERS)
from payload import savings_payload
from savings import globals as glb

NUMBER_OF_ACCOUNTS = 10


@events.init.add_listener
def on_locust_init(environment, **kwargs):
    print("CREATING A PRODUCT AND CLIENT FOR GLOBAL")
    glb.client_id = capi.create_and_return_client()['id']
    glb.product_id = spi.create_and_return_product()['id']

    # iterate 10 times
    for i in range(NUMBER_OF_ACCOUNTS):
        print("CREATING A SAVINGS ACCOUNT %s" % i)
        saving_id = spi.create_and_return_savings_account(glb.product_id, glb.client_id)['id']
        spi.activate_saving_account(saving_id)
        glb.saving_ids.append(saving_id)

    print(glb.saving_ids)


class SavingWithNAccountsTest(HttpUser):
    host = FINERACT_FULL_API
    insecure = True

    @task(3)
    def deposit_task(self):
        saving_id = random.choice(glb.saving_ids)
        self.client.post('/savingsaccounts/%s/transactions?command=deposit' % saving_id,
                         json=savings_payload.transaction(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)

    @task(1)
    def withdraw_task(self):
        saving_id = random.choice(glb.saving_ids)
        self.client.post('/savingsaccounts/%s/transactions?command=withdrawal' % saving_id,
                         json=savings_payload.transaction(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)
