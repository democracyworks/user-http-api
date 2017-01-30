FROM clojure:lein-2.7.1-alpine

RUN mkdir -p /usr/src/user-http-api
WORKDIR /usr/src/user-http-api

COPY project.clj /usr/src/user-http-api/

ARG env=production

RUN lein with-profile $env deps

COPY . /usr/src/user-http-api

RUN lein with-profiles $env,test test
RUN lein with-profile $env uberjar

CMD java ${JVM_OPTS:--XX:+UseG1GC} \
    -javaagent:resources/jars/com.newrelic.agent.java/newrelic-agent.jar \
    -jar target/user-http-api.jar
