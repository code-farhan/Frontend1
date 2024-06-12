CircleCI currently offers beta support for running Docker within build containers.
Docker is an extremely flexible tool that supports many different use cases. This
article attempts to address several of the most popular uses for Docker on CircleCI,
but it is not an exhaustive list. Please [contact us](mailto:sayhi@circleci.com)
if you have questions about uses of Docker that are not covered here.

Note that this article assumes some knowledge of Docker. If you are just getting started
with Docker, then take a look at the [Docker docs](http://docs.docker.com/userguide/)
first.

### Basic usage

To use Docker on CircleCI, simply add Docker as a required service in your
`circle.yml` file like this:

```
machine:
  services:
    - docker
```

You will then be able to use the `docker` command throughout your
`circle.yml` file. Note that you don't need to use `sudo`
to use the command on CircleCI.

### Deployment to a Docker registry

One key use of Docker on CircleCI is to use Docker to build base images to deploy to a
registry like [Docker Hub.](https://hub.docker.com/)
You can do this with the usual`docker build` and `docker push`
commands to build and deploy your image.

Here is an example of a `circle.yml`
file that builds the standard ElasticSearch Docker image and deploys it to
Docker Hub:

```
machine:
  services:
    - docker

dependencies:
  override:
    - docker info
    - docker build -t circleci/elasticsearch .

test:
  override:
    - docker run -d -p 9200:9200 circleci/elasticsearch; sleep 10
    - curl --retry 10 --retry-delay 5 -v http://localhost:9200

deployment:
  hub: 
    branch: master
    commands:
      - sed "s/<EMAIL>/$DOCKER_EMAIL/;s/<AUTH>/$DOCKER_AUTH/" \
          < .dockercfg.template > ~/.dockercfg
      - docker push circleci/elasticsearch
```

Note that a `.dockercfg` file must be dropped in place in order to skip interactive login to
the registry. You should use [environment variables](/docs/environment-variables)
as in the example above to store sensitive information.

For a complete example of building and deploying a Docker image to a 
registry, see the [circleci/docker-elasticsearch](https://github.com/circleci/docker-elasticsearch)
example project on GitHub.

### Application deployment

Another very important use case for Docker containers is "Dockerizing"
applications for deployment purposes. There are countless languages
and technologies that can be deployed this way to a number of hosts that
support Docker containers, but just a couple of examples are provided below
for AWS Elastic Beanstalk and Google Compute Engine with Kubernetes.

#### AWS Elastic Beanstalk

The example below demonstrates building and
testing a Dockerized Rails app and deploying the built image to 
[AWS Elastic Beanstalk](http://aws.amazon.com/elasticbeanstalk/)
(in fact, you can't even really tell that it's a Rails app because all of the
specifics of the app are handled by the Dockerfile). At a high level, this example
does the following:

1.  Builds a Docker image for the application based on the steps in its Dockerfile
    and tags it with the current git commit SHA
2.  Creates a new container based on that image with the
    `docker run`
    command and starts the app inside it
3.  Runs a (very simple) test against the container
4.  Deploys the freshly built image to Docker Hub
5.  Creates a new AWS Elastic Beanstalk "application version" based on the new image
    by referencing the tag
6.  Updates the Elastic Beanstalk environment to use the new version

```
# circle.yml
machine:
  python:
    version: 2.7.3
  services:
    - docker

dependencies:
  pre:
    - pip install awscli
    - docker build -t circleci/hello:$CIRCLE_SHA1 .

test:
  post:
    - docker run -d -p 3000:3000 -e "SECRET_KEY_BASE=abcd1234" circleci/hello:$CIRCLE_SHA1; sleep 10
    - curl --retry 10 --retry-delay 5 -v http://localhost:3000

deployment:
  elasticbeanstalk:
    branch: master
    commands:
      - sed "s/<EMAIL>/$DOCKER_EMAIL/;s/<AUTH>/$DOCKER_AUTH/" < .dockercfg.template > ~/.dockercfg
      - bash -x deploy.sh $CIRCLE_SHA1
```

```
# deploy.sh
#! /bin/bash

SHA1=$1

# Deploy image to Docker Hub
docker push circleci/hello:$SHA1

# Create new Elastic Beanstalk version
EB_BUCKET=hello-bucket
DOCKERRUN_FILE=$SHA1-Dockerrun.aws.json
sed "s/<TAG>/$SHA1/" < Dockerrun.aws.json.template > $DOCKERRUN_FILE
aws s3 cp $DOCKERRUN_FILE s3://$EB_BUCKET/$DOCKERRUN_FILE
aws elasticbeanstalk create-application-version --application-name hello \
  --version-label $SHA1 --source-bundle S3Bucket=$EB_BUCKET,S3Key=$DOCKERRUN_FILE

# Update Elastic Beanstalk environment to new version
aws elasticbeanstalk update-environment --environment-name hello-env \
    --version-label $SHA1
```

Note that Elastic Beanstalk
also allows you to deploy a Dockerfile and associated source code instead of a built image,
but pre-building the image on CircleCI and running some form of verification on it allows
your deployments to be more deterministic because you remove the build environment as a
variable that differs between test and production.

To see the complete source code for the application from this example,
see [circleci/docker-hello](https://github.com/circleci/docker-hello)
on GitHub.

#### Google Compute Engine and Kubernetes

This example shows how to build and deploy a Dockerized application
to [Google Compute Engine](https://cloud.google.com/products/compute-engine/)
using the [Kubernetes](https://github.com/GoogleCloudPlatform/kubernetes)
container cluster manager. This example uses the same Rails application
as the Elastic Beanstalk example above. At a high level, it consists of
the following steps:

1.  Builds a Docker image for the application based on the steps in its Dockerfile
    and tags it with the current git commit SHA
2.  Creates a new container based on that image with the
    `docker run`
    command and starts the app inside it
3.  Runs a (very simple) test against the container
4.  Deploys the freshly built image to a private Docker registry hosted on Google
    Compute Engine (See [google/docker-registry](https://registry.hub.docker.com/u/google/docker-registry/)
    for more information on setting this up)
5.  Updates the Kubernetes replicationController for the Rails app
    to point to the new image based on its SHA
6.  Triggers a rolling update of the pods managed by the Kubernetes controller
    to ensure that the latest image is running on all of them

```
# circle.yml
machine:
  services:
    - docker

dependencies:
  cache_directories:
    - ~/kubernetes
  pre:
    - ./ensure-kubernetes-installed.sh
    - docker build -t $EXTERNAL_REGISTRY_ENDPOINT/hello:$CIRCLE_SHA1 .

test:
  post:
    - docker run -d -p 3000:3000 -e "SECRET_KEY_BASE=abcd1234" \
        $EXTERNAL_REGISTRY_ENDPOINT/hello:$CIRCLE_SHA1; sleep 10
    - curl --retry 10 --retry-delay 5 -v http://localhost:3000

deployment:
  prod:
    branch: master
    commands:
      - ./deploy.sh
```

```
# deploy.sh
#!/bin/bash

# Exit on any error
set -e

KUBE_CMD=${KUBERNETES_ROOT:-~/kubernetes}/cluster/kubecfg.sh

# Create credential files
envsubst < .kubernetes_auth.template > ~/.kubernetes_auth
envsubst < .dockercfg.template > ~/.dockercfg

# Deploy image to private GCS-backed registry
docker push $EXTERNAL_REGISTRY_ENDPOINT/hello:$CIRCLE_SHA1

# Update Kubernetes replicationController
envsubst < kubernetes/rails-controller.json.template > rails-controller.json
$KUBE_CMD -c rails-controller.json \
  update replicationControllers/railscontroller

# Roll over Kubernetes pods
$KUBE_CMD rollingupdate railscontroller
```

This example assumes that you have already launched a Kubernetes cluster
using something like the `cluster/kube-up.sh` script in the Kubernetes
source. See the README in [GoogleCloudPlatform/kubernetes](https://github.com/GoogleCloudPlatform/kubernetes)
for more detailed instructions on setting up a Kubernetes cluster manager.

It is also assumed in this example that a private Docker registry is hosted
on Google Compute Engine, which is available to Kubernetes minions
[without authentication](https://github.com/GoogleCloudPlatform/kubernetes/issues/499)
by using an internally accessible registry endpoint (i.e.
a port that is protected by the firewall but internally accessible).
The registry can be made available
to CircleCI via a public-facing proxy server protected with SSL and HTTP
basic auth. (Note that [this currently requires trusted SSL certificate](https://github.com/docker/docker/pull/5817)).

To see the complete example project using Google Compute Engine and Kubernetes,
see [circleci/docker-hello-google](https://github.com/circleci/docker-hello-google)
for the project source.

### Running tests in a container

Another use case for Docker on CircleCI is running tests inside of or against
a Docker container. All of the usual Docker commands are available within
your test environment on CircleCI, so if you want to run all of your tests
within a Docker container, then you can simply run the container with the
commands necessary to execute your tests, like this:

```
test:
  override:
    - docker run myimage bundle exec rake test
```

You can also start a container running in detached mode with an exposed
port serving your app as in the other examples above. You can then run
browser tests or other black box tests against the container (in place of
the simple `curl` command in the examples).
Currently, you will not enjoy all the benefits of CircleCI's automatic
test parallelization when running tests within a Docker container. So
if you have a lot of unit tests that take a long time to execute, then
you may want to run them outside of the container and only do certain
integration tests against the built Docker image.
Please don't hesitate to [contact us](mailto:sayhi@circleci.com)
if you have any questions at all about how to best utilize Docker on
CircleCI.
