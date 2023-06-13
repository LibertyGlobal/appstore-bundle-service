# AppStore Bundle Service

How to start the appstore-bundle-service application
---

Prerequisites:

1. Start postgres (as a docker instance):
```
docker run --rm -d -p 5432:5432 -e "POSTGRES_HOST_AUTH_METHOD=trust" --name postgres postgres:12
```

2. Start RabbitMQ (as a docker instance):
```
docker run --rm -d -p 5672:5672 --name rabbitmq rabbitmq:3.8.19-management-alpine
docker exec -it rabbitmq rabbitmqadmin declare queue name=bundlegen-service-requests
docker exec -it rabbitmq rabbitmqadmin declare queue name=bundlegen-service-status
docker exec -it rabbitmq rabbitmqadmin declare queue name=bundlecrypt-service-requests
docker exec -it rabbitmq rabbitmqadmin declare queue name=bundlecrypt-service-status
```
Run AppStore Bundle Service:

1. Start application
```
docker run --rm -d -p 8080:8080 --name appstore-bundle-service daccloud/appstore-bundle-service:latest
```

Run AppStore Bundle Service from code:

1. Run `mvn clean package` to build your application
2. Start application on local machine with `mvn spring-boot:run -pl appstore-bundle-service-application -P development`
3. To check that your application is running enter url `http://localhost:8081`

Integration tests
---
**How to run integration tests?**

Navigate to the parent maven module `appstore-bundle-service` and:
* In order to execute tests only against mocked application the `integration-tests` profile should be activated
    * `mvn clean verify -Pintegration-tests`
    <br>or
    * `mvn clean verify -Dci-stage=integration-tests`

**How to generate Allure test report?**

1. After tests finished, navigate to the `appstore-bundle-service-tests` module
2. Run `mvn -pl appstore-bundle-service-test allure:serve`


Install on kubernetes:

1. Create a file `appstore-bundle-service.yaml` and set properties

```yaml 
apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: appstore-bundle-service
  namespace: <namespace>
spec:
  chart:
    repository: http://libertyglobal.github.io/appstore-bundle-service/charts/
    name: appstore-bundle-service
    version: <version>
  values:
    ingress:
      domainName: <cluster_dns_name>
    sealedSecret:
      JDBC_PASSWORD: <jdbc_password>
      JDBC_USER: <jdbc_username>
    configMap:
      JDBC_DATABASE_NAME: <database_name>
      ENVIRONMENT: <environment>
      RABBITMQ_HOST: <rabbit_mq_host>
      APPSTORE_METADATA_SERVICE_URL: <appstore_metadata_url>
```

2. Run `kubectl apply -f appstore-bundle-service.yaml`