from pprint import pprint

import requests

from common import helpers
from common.config import FINERACT_HEADERS, FINERACT_FULL_BE_API
from payload import client_payload

CLIENT_API = FINERACT_FULL_BE_API + '/clients'


def create_client(name, code):
    client_json = client_payload.client(name, code)

    print("     ---> Creating client: " + name)

    response = requests.post(CLIENT_API,
                             json=client_json,
                             headers=FINERACT_HEADERS)

    assert response.status_code == 200

    return response.json()


def list_client(params):
    response = requests.get(CLIENT_API,
                            headers=FINERACT_HEADERS,
                            params=params)

    assert response.status_code == 200

    return response.json()


def find_client_with_last_name(client_list, last_name):
    # pprint(client_list)
    return next((client for client in client_list['pageItems'] if client['lastname'] == last_name), None)


def create_and_return_client():
    last_name = "Doe " + helpers.timeStamp()
    code = "786YYH7 " + helpers.timeStamp()

    client = create_client(last_name, code)
    return {"id": client['resourceId']}
    # helpers.sleep(2)
    # return find_client_with_last_name(list_client({"externalId": code}), last_name)
