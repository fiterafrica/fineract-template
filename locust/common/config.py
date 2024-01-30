from base64 import b64encode

FINERACT_HEADERS = {"Authorization": "Basic %s" % b64encode(b"mifos:password").decode("ascii"),
                    "Fineract-Platform-TenantId": "default",
                    "X-FINERACT-CLIENT-ID": "xyz",
                    "Accept": "application/json",
                    "Content-Type": "application/json"}

# FINERACT_FULL_API = "https://localhost:8443/fineract-provider/api/v1"
# FINERACT_FULL_BE_API = "http://localhost:8081/fineract-provider/api/v1"

FINERACT_FULL_API = "https://cba-staging-eks.getcarbon.co/fineract-provider/api/v1"
