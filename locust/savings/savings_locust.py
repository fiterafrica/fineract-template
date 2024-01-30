from locust import HttpUser, task, events

import client.client_api as capi
import savings.savings_api as spi
from common import helpers
from common.config import (FINERACT_FULL_API, FINERACT_HEADERS)
from payload import savings_payload
from savings import globals as glb


@events.init.add_listener
def on_locust_init(environment, **kwargs):
    print("CREATING A PRODUCT AND CLIENT FOR GLOBAL")
    glb.client_id = capi.create_and_return_client()['id']
    glb.product_id = spi.create_and_return_product()['id']


class SavingTest(HttpUser):
    host = FINERACT_FULL_API
    insecure = True

    # noinspection PyAttributeOutsideInit
    def on_start(self):
        print("CREATING SAVINGS ACCOUNT")
        self.saving_id = spi.create_and_return_savings_account(glb.product_id, glb.client_id)['id']
        spi.activate_saving_account(self.saving_id)

    @task(3)
    def deposit_task(self):
        self.client.post('/savingsaccounts/%s/transactions?command=deposit' % self.saving_id,
                         json=savings_payload.deposit(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)

    @task(1)
    def withdraw_task(self):
        self.client.post('/savingsaccounts/%s/transactions?command=withdrawal' % self.saving_id,
                         json=savings_payload.deposit(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)
