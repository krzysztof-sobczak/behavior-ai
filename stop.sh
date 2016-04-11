#!/bin/bash

BEHAVIORAI=`pwd`

cd ${BEHAVIORAI}/behavior-ai-storage
docker-compose stop
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-kibana
docker-compose stop
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-collector
docker-compose stop
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-frontapp
docker-compose stop
cd ${BEHAVIORAI}