from locust import HttpUser, task

from common.config import (FINERACT_FULL_API, FINERACT_HEADERS)
from common.helpers import timeStamp
from payload import client_payload


class ClientTest(HttpUser):
    host = FINERACT_FULL_API
    insecure = True

    CLIENT_PAYLOAD = {"officeId": 1,
                      "firstname": "Petra",
                      "lastname": "Yton",
                      "externalId": "786YYH7",
                      "dateFormat": "dd MMMM yyyy",
                      "locale": "en",
                      "active": "true",
                      "activationDate": "04 March 2021",
                      "submittedOnDate": "04 March 2021",
                      "legalFormId": 1}

    @task
    def create_client(self):
        json = client_payload.client("LT_John " + timeStamp(), "786YYH7-" + timeStamp())

        self.client.post('/clients',
                         json=json,
                         headers=FINERACT_HEADERS)
