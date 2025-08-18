FROM docker.io/clojure:temurin-21-tools-deps-bookworm-slim AS builder

WORKDIR /opt

COPY . .

RUN cd site && clj -Sdeps '{:mvn/local-repo "./.m2/repository"}' -T:build uber

FROM docker.io/library/eclipse-temurin:24 AS runtime
COPY --from=builder /opt/site/target/site-0.0.1-standalone.jar /site.jar

EXPOSE 3000

ENTRYPOINT ["java", "-cp", "site.jar", "clojure.main", "-m", "site.server", "3000"]
