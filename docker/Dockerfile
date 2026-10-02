# ============================================================
# 申込管理システム アプリケーションイメージ（Tomcat 9 / Java 17）
# 1) Maven で WAR をビルド  2) Tomcat 9 に ROOT.war として配置
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM tomcat:9.0-jdk17-temurin
ENV TZ=Asia/Tokyo \
    JAVA_OPTS="-Duser.timezone=Asia/Tokyo -Dfile.encoding=UTF-8"
# 既定の webapps（manager 等）は使わない
RUN rm -rf /usr/local/tomcat/webapps/* \
 && sed -i 's/<Connector port="8080" protocol="HTTP\/1.1"/<Connector port="8080" protocol="HTTP\/1.1" URIEncoding="UTF-8"/' /usr/local/tomcat/conf/server.xml
COPY --from=build /build/target/appmgmt.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
