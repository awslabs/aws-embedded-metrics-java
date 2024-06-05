# Examples

## Lambda

You can deploy the Lambda example by running:

```sh
export AWS_REGION=us-west-2
export LAMBDA_ARN="arn:aws:lambda:$AWS_REGION:<AccountId>:function:<FunctionName>"
./examples/lambda/deploy/deploy-lambda.sh $LAMBDA_ARN $AWS_REGION
```

## Agent

In order to run this example you will need the CloudWatch Agent running locally. 
The easiest way to do this is by running it in a Docker container using the following script.
Alternatively, you can find installation instructions [here](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Agent-on-EC2-Instance.html).

```sh
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_REGION=us-west-2
./bin/start-agent.sh
```

Run the example:

```
./examples/agent/bin/run.sh
```

## Docker

With Docker images, using the `awslogs` log driver will send your container logs to CloudWatch Logs. All you have to do is write to STDOUT and your EMF logs will be processed.

[Official Docker documentation for `awslogs` driver](https://docs.docker.com/config/containers/logging/awslogs/)

## ECS and Fargate

With ECS and Fargate, you can use the `awslogs` log driver to have your logs sent to CloudWatch Logs on your behalf. After configuring your task to use the `awslogs` log driver, you may write your EMF logs to STDOUT and they will be processed.
To write your EMF logs to STDOUT, set the environment variable `WRITE_TO_STDOUT=true`

[ECS documentation on `awslogs` log driver](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_awslogs.html)

## Fluent Bit and Fluentd

Fluent Bit can be used to collect logs and push them to CloudWatch Logs. After configuring the Amazon CloudWatch Logs output plugin, you may write your EMF logs to STDOUT and they will be processed.

[Getting Started with Fluent Bit](https://docs.fluentbit.io/manual/installation/getting-started-with-fluent-bit)

[Amazon CloudWatch output plugin for Fluent Bit](https://docs.fluentbit.io/manual/pipeline/outputs/cloudwatch)

## FireLens on ECS EC2

You can deploy the example by running the following:

```sh
# create an ECR repository for the example image
aws ecr create-repository --repository-name <image-name> --region <region>

# create an S3 bucket for the Fluent-Bit configuration
aws s3api create-bucket --bucket <bucket-name> --region <region>

# create ECS cluster
# create ECS task definition
# create ECS service

# deploy
./examples/ecs-firelens/bin/publish.sh \
  <account-id> \
  <region> \
  <image-name> \
  <s3-bucket> \
  <ecs-cluster-name> \
  <ecs-task-family> \
  <ecs-service-name>
```  

## FireLens on ECS Fargate

For running on Fargate, s3 file option is not supported for Fluent-bit config. Hence, we need to build the fluent-bit custom config image and then use its reference in our Firelens container definition.

For building the custom fluent-bit image, clone the [amazon-ecs-firelens-examples](https://github.com/aws-samples/amazon-ecs-firelens-examples) and modify the contents of [extra.conf](https://github.com/aws-samples/amazon-ecs-firelens-examples/blob/mainline/examples/fluent-bit/config-file-type-file/extra.conf) in the amazon-ecs-firelens-examples repository. Post this run the following commands to build the custom fluent-bit image:-

```sh 
# create an ECR repository for the Fluentbit-config image
aws ecr create-repository --repository-name <config-image-name> --region <region>

# Navigate to the config file directory
cd examples/fluent-bit/config-file-type-file

# Build the docker image from Dockerfile. Replace config-image-name with your image name
docker build -t <config-image-name> .

# Tag the recently built docker image . Replace the config-image-name, account-id and region with your values.
docker tag <config-image-name>:latest <account-id>.dkr.ecr.<region>.amazonaws.com/<config-image-name>:latest

# Push the docker image to ECR
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/<config-image-name>:latest
```

For executing EMF application on Fargate, you need to execute the following commands :-

```sh 
# create an ECR repository for the example image
aws ecr create-repository --repository-name <image-name> --region <region>

# create ECS cluster
# create ECS task definition
# create ECS service

# deploy
./examples/ecs-firelens/bin/publish-fargate.sh 
<account-id> \
<region> \
<example-image> \
<fargate-config-image> \
<ecs-cluster> \
<ecs-task> \
<ecs-service>

```
