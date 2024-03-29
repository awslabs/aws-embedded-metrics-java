#!/usr/bin/env bash
# Usage:
#   ./examples/ecs-firelens/bin/publish-fargate.sh \
#     <account-id> \
#     <region> \
#     <image-name> \
#     <config-image-name> \
#     <ecs-cluster-name> \
#     <ecs-task-family> \
#     <ecs-service-name>

rootdir=$(git rev-parse --show-toplevel)

ACCOUNT_ID=$1
REGION=$2
IMAGE_NAME=$3 # emf-ecs-firelens
FLUENT_BIT_CONFIG=$4
CLUSTER_NAME=$5 # emf-example
ECS_TASK_FAMILY=$6 # aws-emf-ecs-app-example
ECS_SERVICE_NAME=$7 # aws-emf-ecs-firelens-ec2

LIB_PATH=$rootdir
EXAMPLE_DIR=$rootdir/examples/ecs-firelens
ECR_REMOTE=$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$IMAGE_NAME

function check_exit() {
    last_exit_code=$?
    if [ $last_exit_code -ne 0 ];
    then
        echo "Last command failed with exit code: $last_exit_code."
        echo "Exiting."
        exit $last_exit_code;
    fi
}

echo 'BUILDING THE LOCAL PROJECT'
pushd $rootdir
./gradlew :examples:ecs-firelens:build
check_exit
popd

pushd $EXAMPLE_DIR
pwd


echo 'UPDATING CONTAINER DEFINITIONS'
sed "s/<account-id>/$ACCOUNT_ID/g" $EXAMPLE_DIR/container-definitions-fargate.template.json \
| sed "s/<region>/$REGION/g" \
| sed "s/<Firelens-custom-conf-image>/$FLUENT_BIT_CONFIG/g" \
| sed "s/<image-name>/$IMAGE_NAME/g" \
> $EXAMPLE_DIR/container-definitions.json
check_exit

echo 'BUILDING THE EXAMPLE DOCKER IMAGE'
`aws ecr get-login --no-include-email --region $REGION`
docker build . -t $IMAGE_NAME:latest
check_exit

echo 'PUSHING THE EXAMPLE DOCKER IMAGE TO ECR'
imageid=$(docker images -q $IMAGE_NAME:latest)
docker tag $imageid $ECR_REMOTE
docker push $ECR_REMOTE
check_exit

echo 'UPDATING THE ECS SERVICE'
aws ecs update-service \
  --region $REGION \
  --cluster $CLUSTER_NAME \
  --service $ECS_SERVICE_NAME \
  --force-new-deployment \
  --task-definition $(aws ecs register-task-definition \
                        --network-mode awsvpc \
                        --task-role arn:aws:iam::$ACCOUNT_ID:role/ecsTaskExecutionRole \
                        --execution-role-arn "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskExecutionRole" \
                        --region $REGION \
                        --memory 512 \
                        --cpu 256 \
                        --family $ECS_TASK_FAMILY \
                        --container-definitions "$(cat container-definitions.json)" \
                    | jq --raw-output '.taskDefinition.taskDefinitionArn' | awk -F '/' '{ print $2 }')

popd
