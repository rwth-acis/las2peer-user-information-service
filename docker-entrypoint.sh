#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' gradle.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' gradle.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' gradle.properties)
export CORE_VERSION=$(awk -F "=" '/core.version/ {print $2}' gradle.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}

# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi
echo external_address = $(curl -s https://ipinfo.io/ip):${LAS2PEER_PORT} > etc/pastry.properties
# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* -jar lib/las2peer-bundle-'"${CORE_VERSION}"'.jar -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
  exec ${LAUNCH_COMMAND} --observer startService\("'""${SERVICE}""'"\) startWebConnector
else
  exec ${LAUNCH_COMMAND} ${@}
fi
