import json

import websocket


# websocket._logging._logger.level = -99
# logger = logging.getLogger('websocket')
# logger.setLevel(logging.INFO)
# logger.addHandler(logging.StreamHandler())

def fn_on_message_handler(wsapp, message):
    print(message)


def fn_on_error(ws, error):
    print(error)


def fn_on_close(ws):
    print("### closed ###")


ws = websocket.WebSocket()
ws.connect("wss://fineract-dev.vfdbank.systems/fineract-provider/camel/fineract-result", origin="fineract-dev.vfdbank.systems",
           on_message=fn_on_message_handler,
           on_error=fn_on_error,
           on_close=fn_on_close)

ws.send(json.dumps({"tenantId": "uat", "username": "mifos", "password": "oppolife2019"}))

while True:
    print(ws.recv())

# map dict to json
