#!/bin/bash
# Quick wrapper for running with some easy arguments
usage() {
  echo 'run.sh [--port PORT] [--id ID] [--config CONFIG] [--debug]'
  echo ''
  echo -e 'PORT\tThe port to serve the REST interface on'
  echo -e 'ID\tThe client-id to use'
  echo -e 'CONFIG\tThe config file to use to override defaults'
  echo -e '--debug\tSet primary log levels to DEBUG'
}

SCALA_VERSION='2.12'
APP='Snotify'
VERSION='0.0.1-SNAPSHOT'
JARFILE="target/scala-$SCALA_VERSION/$APP-assembly-$VERSION.jar"

PORT_ARG=''
ID_ARG=''
CONFIG_ARG=''
DEBUG_ARG=''

while [[ "$1" != '' ]]; do
  case "$1" in
    --config|--cfg|-c)
      shift
      CONFIG_ARG="-Dapp.config=$1"
      shift
      ;;
    --port|-p)
      shift
      PORT_ARG="-Drest.port=$1"
      shift
      ;;
    --id|-i)
      shift
      ID_ARG="-Dclient-id=$1"
      shift
      ;;
    --debug)
      shift
      DEBUG_ARG='-Dlogback.levels.xantoria=DEBUG -Dlogback.levels.main=DEBUG'
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

JVM_ARGS="$PORT_ARG $ID_ARG $CONFIG_ARG $DEBUG_ARG"

java $JVM_ARGS -jar "$JARFILE"
