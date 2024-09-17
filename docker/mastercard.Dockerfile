FROM  eclipse-temurin:21-jre-alpine

ENV VERSION 2.0.1

WORKDIR /app/mastercard/

ADD mastercard_inbound-$VERSION.jar $VERSION.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","2.0.1.jar", "--server"]