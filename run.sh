#!/bin/bash

BEHAVIORAI=`pwd`

cd ${BEHAVIORAI}/behavior-ai-storage
docker-compose up -d
cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-kibana
docker-compose up -d
cd ${BEHAVIORAI}

#cd ${BEHAVIORAI}/behavior-ai-collector
#docker-compose up -d
#cd ${BEHAVIORAI}

cd ${BEHAVIORAI}/behavior-ai-frontapp
docker-compose up -d
cd ${BEHAVIORAI}