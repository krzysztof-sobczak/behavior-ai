#!/bin/bash

./stop.sh

BEHAVIORAI=`pwd`

cd ${BEHAVIORAI}/behavior-ai-storage
docker-compose rm -f
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-kibana
docker-compose rm -f
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-collector
docker-compose rm -f
cd ${BEHAVIORAI}