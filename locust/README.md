


Load tests are run using locust to run the test first install the required dependencies.

1. Install python 3
2. Install the requirement by running `pip3 -r requirements.txt`

### Running the tests

For now, you can only run tests for one module at a time.

To run tests for the clients run `>locust -f client/client_locust.py`

And for saving account deposits run `>locust -f savings/savings_locust.py`

`locust -f savings/single_saving_account_locust.py --headless -r 1 -u 500 -t 5m   --html "[Enterprise] 1_Account 500_Users 5_Minutes.html"`
`locust -f savings/single_saving_account_locust.py --headless -r 5 -u 1000 -t 1s   --html "1_Account 1000_Users 5_Minutes-$(date '+%Y-%m-%d_%H-%M-%S').html"`
