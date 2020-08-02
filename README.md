## My Project

TODO: Fill this README out!

Be sure to:

* Change the title in this README
* Edit your repository description on GitHub

### Testing

We have 2 different types of tests:

1. Unit tests. The command to run these tests

	```sh
	./gradlew test
	```

1. Integration tests. These tests require Docker to run the CloudWatch Agent and valid AWS credentials. Tests can be run by:

	```sh
	export AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY_ID
	export AWS_SECRET_ACCESS_KEY=YOUR_ACCESS_KEY
	export AWS_REGION=us-west-2
	./gradlew integ
	```

	**NOTE**: You need to replace the access key id and access key with your own AWS credentials.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
