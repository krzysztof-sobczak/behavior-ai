#!/usr/bin/env bash

FILEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_PATH="${FILEDIR}/../../..";
PATTERN_FILE="${BASE_PATH}/patterns/app"

TEMPLATE_FILE="${FILEDIR}/template.conf"
TMP_FILE="${FILEDIR}/tmp_category_pattern.conf"
OUTPUT_FILE="${BASE_PATH}/config/2_filters_app_category_pattern.conf"

CATEGORY_PATTERNS=(`awk '{ print $1 }' ${PATTERN_FILE} | xargs echo $1`)

rm -f ${TMP_FILE}
for CATEGORY_PATTERN in "${CATEGORY_PATTERNS[@]}"
do
	sed "s/CATEGORYPATTERN/${CATEGORY_PATTERN}/g" ${TEMPLATE_FILE} >> ${TMP_FILE}
done
mv ${TMP_FILE} ${OUTPUT_FILE}