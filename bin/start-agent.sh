#!/usr/bin/env bash
#
# Run integration tests against a CW Agent.
# We first create the necessary
#
# usage:
#   export AWS_ACCESS_KEY_ID=
#   export AWS_SECRET_ACCESS_KEY=
#   export AWS_REGION=us-west-2
#   ./start-agent.sh

rootdir=$(git rev-parse --show-toplevel)
rootdir=${rootdir:-$(pwd)} # in case we are not in a git repository (Code Pipelines)

tempfile="$rootdir/src/integration-test/resources/agent/.temp"

###################################
# Configure and start the agent
###################################

pushd $rootdir/src/integration-test/resources/agent
echo "[AmazonCloudWatchAgent]
" > ./.aws/credentials

echo "[profile AmazonCloudWatchAgent]
region = $AWS_REGION
" > ./.aws/config

docker build -t agent:latest .
docker run  -p 25888:25888/udp -p 25888:25888/tcp  \
    -e AWS_REGION=$AWS_REGION \
    agent:latest &> $tempfile &
popd
