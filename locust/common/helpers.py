import random
import string
import time


def raise_if_http_error(response):
    if response.status_code != 200:
        raise Exception(f"HTTP Error: {response.status_code} : Message: {response.text}")


def padWithRandomString(s, length):
    """
    Pad a string with random characters
    """
    return s + ''.join(random.choice(string.ascii_uppercase) for _ in range(length - len(s)))


def randomString(n):
    """
    Return a random string of length n
    """
    return ''.join(random.choice(string.ascii_uppercase) for _ in range(n))


def timeStamp():
    """
    Return a timestamp for today in the format YYYY-MM-DD
    """
    from datetime import datetime
    return datetime.now().strftime("%d-%b-%Y (%H:%M:%S.%f)")


def date_string_now():
    """
    Return a timestamp for today in the format YYYY-MM-DD
    """
    from datetime import datetime
    return datetime.now().strftime("%d %B %Y")


def sleep(n):
    # time.sleep(n)
    pass
