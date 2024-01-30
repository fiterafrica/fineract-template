from locust import HttpUser, task

import client.client_api as capi
import loans.loan_api as lpi
from common.config import (FINERACT_FULL_API)


class SavingTest(HttpUser):
    host = FINERACT_FULL_API
    insecure = True

    # noinspection PyAttributeOutsideInit
    def on_start(self):
        print("CREATING A LOAN PRODUCT AND CLIENT")
        self.lapi = lpi
        self.client_id = capi.create_and_return_client()['id']
        self.product_id = self.lapi.create_and_return_product()['id']
        print("Product Id = " + str(self.product_id))

    @task(3)
    def disburse_loan(self):
        # print 10 stars
        print("*" * 10)
        print("DISBURSING A LOAN")
        self.lapi.create_and_return_loan_account(self.product_id, self.client_id)
