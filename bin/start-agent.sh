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

# Store the AWS STS assume-role output and extract credentials directly
CREDS=$(aws sts assume-role \
    --role-arn arn:aws:iam::863722843142:role/CodeBuildExecutionRole \
    --role-session-name "session-$(uuidgen)" \
    --query 'Credentials.[AccessKeyId,SecretAccessKey,SessionToken]' \
    --output text \
    --duration-seconds 3600) 

# Parse the output into separate variables
read AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN <<< $CREDS

# Export the variables
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN

pushd $rootdir/src/integration-test/resources/agent
echo "[AmazonCloudWatchAgent]
aws_access_key_id = $AWS_ACCESS_KEY_ID
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
aws_session_token = $AWS_SESSION_TOKEN
" > ./.aws/credentials

echo "[profile AmazonCloudWatchAgent]
region = $AWS_REGION
" > ./.aws/config

docker build -t agent:latest .
docker run  -p 25888:25888/udp -p 25888:25888/tcp  \
    -e AWS_REGION=$AWS_REGION \
    -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
    -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
    -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
    agent:latest &> $tempfile &
popd
