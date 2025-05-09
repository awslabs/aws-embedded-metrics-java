FROM amazon/cloudwatch-agent

COPY ./.aws/amazon-cloudwatch-agent.json /opt/aws/amazon-cloudwatch-agent/bin/default_linux_config.json
COPY ./.aws/config /root/.aws/config
COPY ./.aws/credentials /root/.aws/credentials

ENV RUN_IN_CONTAINER="True"
ENTRYPOINT ["/opt/aws/amazon-cloudwatch-agent/bin/start-amazon-cloudwatch-agent"]
