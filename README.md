# user-http-api

A REST API gateway for managing users.

[![Build status](https://badge.buildkite.com/cff715ecfc18328abe04bcb114bf9103c1e83f52fcc4b528d6.svg)](https://buildkite.com/democracyworks/user-http-api)

## Configuration

* ALLOWED_ORIGINS
    * This env var controls the cross-origin resource sharing (CORS) settings.
    * It should be set to one of the following:
        * `[".*"]` to allow requests from any origin
        * an EDN seq of allowed origin strings
        * an EDN map containing the following keys and values
            * :allowed-origins - sequence of strings
            * :creds - true or false, indicates whether client is allowed to send credentials
            * :max-age - a long, indicates the number of seconds a client should cache the response from a preflight request
            * :methods - a string, indicates the accepted HTTP methods.  Defaults to "GET, POST, PUT, DELETE, HEAD, PATCH, OPTIONS"
    * For example: `ALLOWED_ORIGINS=["http://foo.example.com" "http://bar.example.com"]`

## Usage

### Create

POST to / with EDN or Transit payload matching the schema below:

```clojure
{:first-name "string"
 :last-name "string"
 :email "string" ; optional if phone isn't blank
 :phone "string" ; optional if email isn't blank
 :addresses {:type {:street "string"
                    :street-2 "string"
                    :city "string"
                    :state "string"
                    :zip "string"}}
```

### Read

GET to /user-uuid

### Update

PUT or PATCH to /user-uuid with EDN or Transit payload matching the schema below:

```clojure
{:id #uuid "users-uuid-string"
 :any-valid-create-field "new-string-value"}
```

### Delete

DELETE to /user-uuid

## Running

### With docker-compose

Build it:

```
> docker-compose build
```

Run it:

```
> docker-compose up
```

TODO: If any more env vars are needed, add them to docker-compose command above.

#### Connecting up a Cider REPL

1. Run with `docker-compose up`.
1. Figure out your boot2docker ip address, running `boot2docker ip`
can tell you this.
1. In emacs, run `M-x cider-connect`.
1. When prompted for host, enter the boot2docker ip.
1. When prompted for port, enter 1527 (as configured in the Dockerfile
and docker-compose.yml)
1. Open any namespace.
1. `C-c C-k` to compile it.
1. `C-c M-n` to place the REPL in your current namespace.
1. Switch to your REPL buffer.
1. Execute code to your heart's content.

You can make code changes, recompile, and those changes will be loaded
in the running JVM within Wildfly within the Docker container. You are
hot patching new Clojure code into the running JVM instance. As long as
you bind the NREPL server to a unique port per project you can have
different Cider sessions simultaneously connected to different projects
at the same time.

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
