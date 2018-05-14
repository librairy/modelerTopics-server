FROM librairy/modeler-topics-service:latest
ADD model /bin
ENV SWAGGER_TITLE="CORDIS Topic Model - Infrastructure as Category"
ENV SWAGGER_DESCRIPTION="Topic Model created from Community Research and Development Information Service projects between 1984-2018"
ENV SWAGGER_CONTACT_NAME="librAIry"
ENV SWAGGER_CONTACT_EMAIL="cbadenes@fi.upm.es"
ENV SWAGGER_CONTACT_URL="http://librairy.linkeddata.es"
ENV SWAGGER_LICENSE_NAME="Apache License Version 2.0"
ENV SWAGGER_LICENSE_URL="https://www.apache.org/licenses/LICENSE-2.0"
ENTRYPOINT exec java $JAVA_OPTS -server -jar /app.jar
