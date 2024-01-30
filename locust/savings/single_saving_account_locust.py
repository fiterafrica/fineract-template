from locust import HttpUser, task, events, between

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
    glb.saving_id = spi.create_and_return_savings_account(glb.product_id, glb.client_id)['id']
    spi.activate_saving_account(glb.saving_id)

    # glb.saving_id = 28


class SavingTest(HttpUser):
    host = FINERACT_FULL_API
    insecure = True

    # wait_time = between(0.5, 1)

    @task(3)
    def deposit_task(self):
        self.client.post('/savingsaccounts/%s/transactions?command=deposit' % glb.saving_id,
                         json=savings_payload.deposit(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)

    @task(1)
    def withdraw_task(self):
        self.client.post('/savingsaccounts/%s/transactions?command=withdrawal' % glb.saving_id,
                         json=savings_payload.deposit(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)
