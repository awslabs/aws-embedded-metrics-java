# Examples

## Lambda

You can deploy the Lambda example by running:

```sh
export AWS_REGION=us-west-2
export LAMBDA_ARN="arn:aws:lambda:$AWS_REGION:<AccountId>:function:<FunctionName>"
./examples/lambda/deploy/deploy-lambda.sh $LAMBDA_ARN $AWS_REGION
```
    
## TODO: Add more environments