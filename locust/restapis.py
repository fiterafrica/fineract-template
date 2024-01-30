#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

import random
from datetime import datetime
from base64 import b64encode
from locust import HttpUser, TaskSet, tag, task, SequentialTaskSet

from common import helpers
from common.config import (FINERACT_FULL_API, FINERACT_HEADERS)
from payload import savings_payload

# basePath = "fineract-provider/api/v1"
# domain = "fineract-dev.vfdbank.systems"
# port = "443"
# protocol = "https"
# depositPayload = {"transactionDate":"16 June 2022","transactionAmount":50,"locale":"en","dateFormat":"dd MMMM yyyy"}
# withdrawPayload = {"transactionDate":"16 June 2022","transactionAmount":10,"locale":"en","dateFormat":"dd MMMM yyyy"}
# headers = {"Authorization": "Basic %s" % b64encode(b"mifos:oppolife2019").decode("ascii"), "Fineract-Platform-TenantId": "default", "X-FINERACT-CLIENT-ID": "xyz", "Accept": "application/json", "Content-Type": "application/json"}
with open("savings_accounts.txt") as f:
    ids = [line.strip() for line in f]

with open("client_accounts.txt") as f:
    client_ids = [line.strip() for line in f]

class FineractAsyncTaskSet(SequentialTaskSet):

    # @tag('savings')
    # @tag('deposit')
    # @task(100)
    # def depositSavings(self):
    #     id = random.choice(ids)
    #     url = "https://%s:%s/%s/savingsaccounts/%s/transactions/async?command=deposit" % (domain, port, basePath, id)
    #     print("url: %s" % url)
    #     response = self.client.post(url=url, data=str(depositPayload), headers=headers, verify=False)
    #     print("deposit response: %s" % response.json())

    @tag('savings')
    @tag('deposit')
    @task(3)
    def deposit_task(self):
        saving_id = random.choice(ids)
        response = self.client.post('/savingsaccounts/%s/transactions?command=deposit' % saving_id,
                         json=savings_payload.transaction(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)
        print("deposit response: %s" % response.json())

    @tag('savings')
    @tag('withdrawal')
    @task(1)
    def withdraw_task(self):
        saving_id = random.choice(ids)
        response = self.client.post('/savingsaccounts/%s/transactions?command=withdrawal' % saving_id,
                         json=savings_payload.transaction(50, helpers.date_string_now()),
                         headers=FINERACT_HEADERS)
        print("withdrawal response: %s" % response.json())

    @tag('client')
    @tag('search')
    @task(1)
    def client_search_task(self):
        response = self.client.post('/clients/search?offset=0&limit=15',
                         json=savings_payload.client_search(),
                         headers=FINERACT_HEADERS)
        print("client search response: %s" % response.json())

    @tag('savings')
    @tag('transaction_search')
    @task(1)
    def transaction_search_task(self):
        client_id = random.choice(client_ids)
        response = self.client.post('/savingsaccounts/transactions/search?limit=100&offset=0',
                         json=savings_payload.deposit_transaction_search(client_id),
                         headers=FINERACT_HEADERS)
        print("transaction_search response: %s" % response.json())

    # @tag('savings')
    # @tag('withdrawal')
    # @task(100)
    # def withdrawSavings(self):
    #     id = random.choice(ids)
    #     url = "https://%s:%s/%s/savingsaccounts/%s/transactions/async?command=withdrawal" % (domain, port, basePath, id)
    #     print("url: %s" % url)
    #     response = self.client.post(url=url, data=str(withdrawPayload), headers=headers)
    #     print("withdrawal response: %s" % response.json())

class FineractAsync(HttpUser):
    tasks = [FineractAsyncTaskSet]
    min_wait = 500
    max_wait = 1000
    host = FINERACT_FULL_API
    insecure = True
