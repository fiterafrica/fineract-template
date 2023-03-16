import os
import sys
import time
import random
from datetime import datetime
from base64 import b64encode
from locust import HttpUser, TaskSet, task, SequentialTaskSet

basePath = "fineract-provider/api/v1"
domain = "localhost"
port = "8443"
depositPayload = {"transactionDate":"31 January 2023","transactionAmount":50,"locale":"en","dateFormat":"dd MMMM yyyy", "paymentTypeId": 1}

withdrawPayload = {"transactionDate":"15 Nov 2022","transactionAmount":10,"locale":"en","dateFormat":"dd MMMM yyyy"}
headers = {"Authorization": "Basic %s" % b64encode(b"mifos:password").decode("ascii"), "Fineract-Platform-TenantId": "default", "X-FINERACT-CLIENT-ID": "xyz", "Accept": "application/json", "Content-Type": "application/json"}
with open("savings_accounts.txt") as f:
    ids = [line.strip() for line in f]

class FineractAsyncTaskSet(SequentialTaskSet):

    @task
    def depositSavings(self):
        id = 312140
        url = "https://%s:%s/%s/savingsaccounts/1/transactions/async?command=deposit" % (domain, port, basePath)
        print("url: %s" % url)
        self.client.verify = False
        response = self.client.post(url=url, data=str(depositPayload), headers=headers)
        print("deposit response: %s" % response.raw)

#     @task
#     def withdrawSavings(self):
#         id = 312140
#         url = "https://%s:%s/%s/savingsaccounts/1/transactions?command=withdrawal" % (domain, port, basePath)
#         print("url: %s" % url)
#         response = self.client.post(url=url, data=str(withdrawPayload), headers=headers, verify=False)
#         print("withdrawal response: %s" % response.raw)

class FineractAsync(HttpUser):
    tasks = [FineractAsyncTaskSet]
    min_wait = 500
    max_wait = 1000
    host = "https://%s:%s" % (domain, port)