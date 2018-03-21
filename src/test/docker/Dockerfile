FROM librairy/modeler-topics-service:latest
ADD bin /bin
ENV SWAGGER_TITLE="Hi Crazy World!"
ENTRYPOINT exec java $JAVA_OPTS -server -jar /app.jar
