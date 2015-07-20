# user-http-api

A REST API gateway for managing users.

[![Build status](https://badge.buildkite.com/cff715ecfc18328abe04bcb114bf9103c1e83f52fcc4b528d6.svg)](https://buildkite.com/democracyworks/user-http-api)

## Configuration

### New config

New configuration on this app should be done in four steps:

1. Add a configuration to the `resources/config.edn` file, using
   `#resource-config/env` tagged literals for environment variables.
2. Add the env var to the `user-http-api@.service.template` docker run
   command, pulling in the value from Consul.
   `--env FAKE_ENV_VAR=$(curl -s http://${COREOS_PRIVATE_IPV4}:8500/v1/kv/user-http-api/fake/env/var?raw)`
3. Set up the value in Consul.
4. Add them to the README.md (this file) in the Running with
   docker-compose section as well as documented in the existing config
   section.

Keys in Consul should be appropriately namespaced, preferably under user-http-api.

### Existing config

TODO: Add user-http-api specific configuration.

## Usage

TODO: Add usage

## Running

### With docker-compose

First, create a `profiles.clj`.

```
> cp profiles.clj.sample profiles.clj
```

Edit the datomic username and password in `profiles.clj` (it won't be
added to git).

Build it:

```
> docker-compose build
```

Run it:

```
> docker-compose up
```

TODO: If any more env vars are needed, add them to docker-compose command above.

### Running in CoreOS

There is a user-http-api@.service.template file provided in the repo. Look
it over and make any desired customizations before deploying. The
DOCKER_REPO, IMAGE_TAG, and CONTAINER values will all be set by the
build script.

The `script/build` and `script/deploy` scripts are designed to
automate building and deploying to CoreOS.

1. Run `script/build`.
1. Note the resulting image name and push it if needed.
1. Set your FLEETCTL_TUNNEL env var to a node of the CoreOS cluster
   you want to deploy to.
1. Make sure rabbitmq service is running.
1. Run `script/deploy`.

## License

Copyright © 2015 Democracy Works, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
