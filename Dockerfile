FROM quay.io/democracyworks/didor:latest

RUN mkdir -p /usr/src/user-http-api
WORKDIR /usr/src/user-http-api

COPY project.clj /usr/src/user-http-api/

RUN lein deps
RUN lein immutant war --name no-code-just-deps --destination target --nrepl-start

COPY . /usr/src/user-http-api

RUN lein test
RUN lein immutant war --name user-http-api --destination target --nrepl-port=1527 --nrepl-start --nrepl-host=0.0.0.0
