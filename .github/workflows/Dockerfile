FROM gradle:7.4-jdk11-alpine as builder

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Seoul
RUN apk --no-cache add tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /build
COPY . /build
RUN gradle build

FROM openjdk:11.0-slim
WORKDIR /qriz
COPY --from=builder /build/build/libs/*.jar .

EXPOSE 8080
CMD ["java", "-jar", "qriz-0.0.1-SNAPSHOT.jar", "--spring.config.location=file:/qriz/resources/"]
