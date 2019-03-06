#!/usr/bin/env bash

[ -z "$IDS_URL" ] && IDS_URL=https://localhost:8089

which jq > /dev/null 2>&1 || echo "jq is required for this demo"

echo "Registering patient ID"
curl -sk \
  -H "Content-Type: application/json" \
  -d '[{"system":"CDW","resource":"PATIENT","identifier":"185601V82529"}]' \
  $IDS_URL/api/v1/ids \
  | jq .

echo -e "\nLooking up patient ID"
curl -sk $IDS_URL/api/v1/ids/c1b51d94-0ae0-526a-8a3b-6ae673991e10 | jq .

echo -e "\nSubmitting a bad request"
curl -sk \
  -w '\nStatus: %{http_code}\n' \
  -o /dev/stdout \
  -H "Content-Type: application/json" \
  -d '[{"system":"CDW","resource":"PATIENT"}]' \
  $IDS_URL/api/v1/ids \
  > /dev/stderr \
  | jq .


