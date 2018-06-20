#!/bin/bash

# Submit a notification via REST

YELLOW=$(tput setaf 3)
RESET=$(tput sgr0)

FIXTURE_ID='no-id'
HOST=localhost
PORT=${SNOTIFY_HTTP_PORT:-18080}
ROOT_DIR="$(dirname $(realpath $0))/.."

# TODO: Support args properly, and show usage info
if [[ "$1" != '' ]]; then
  FIXTURE_ID="$1"
fi

FILE="$ROOT_DIR/src/test/resources/fixtures/notifications/$FIXTURE_ID.json"

if [[ ! -f "$FILE" ]]; then
  echo "Unknown fixture $FILE" 2>&1
  exit 1
fi

echo "$YELLOW"
echo "Submitting fixture via REST interface..."
cat "$FILE"
echo ''
echo "$RESET"

curl \
  -d "@$FILE"\
  -X POST \
  -H 'Content-type: application/json' \
  "http://$HOST:$PORT/notification" -D -

echo ''
