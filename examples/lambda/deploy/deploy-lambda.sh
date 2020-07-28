#!/usr/bin/env bash
#
# usage:
# ./deploy/deploy-lambda.sh { LAMBDA_ARN } { region }

rootdir=$(git rev-parse --show-toplevel)

pushd .
cd $rootdir

# build the lambda example project
./gradlew :examples:lambda:build

popd

ZIP_PATH=$rootdir/examples/lambda/build/distributions/lambda.zip

AWS_LAMBDA_ARN=$1
REGION=$2


###################################
# Deploy Lambda
###################################

echo "Updating function code with archive at $ZIP_PATH..."
aws lambda update-function-code \
    --function-name $AWS_LAMBDA_ARN \
    --region $REGION \
    --zip-file fileb://$ZIP_PATH
