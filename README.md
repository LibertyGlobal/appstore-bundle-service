# AppStore Bundle Service

How to start the appstore-bundle-service application
---

1. Run `mvn clean package` to build your application
1. Start application on local machine with `mvn exec:exec -pl appstore-bundle-service-application -P development`
1. To check that your application is running enter url `http://localhost:8080`

Run locally:

1. Start postgres (as a docker instance):
```
docker run -d -p 5432:5432 -e "POSTGRES_HOST_AUTH_METHOD=trust" postgres:12
```
2. Execute the migration in order to set up the `appstore-bundle-service` database schema:
```
mvn -pl appstore-bundle-service-storage/storage-persistent flyway:migrate -Dpostgres-embedded.port=5432 -Dpostgres-embedded.host=127.0.0.1 -Dpostgres-embedded.username=postgres
```
3. Run rabbitmq locally. In case it runs in kube env:
```
kubectl port-forward service/bundle-generator-rabbit 5672:5672
```
4. Use "dev" profile. Provide JVM parameter:
```
-Dspring.profiles.active=dev
```

Integration tests
---
**How to run integration tests?**

Navigate to the parent maven module `appstore-bundle-service` and:
* In order to execute tests only against mocked application the `integration-tests` profile should be activated
    * `mvn clean verify -Pintegration-tests`
    <br>or
    * `mvn clean verify -Dci-stage=integration-tests`
* In order to run tests also against deployed service on k8s, include the service URL parameter: <br>`mvn clean verify -Dci-stage=integration-tests -Dservice.url=https://your.service.url.here`

**How to generate Allure test report?**

1. After tests finished, navigate to the `appstore-bundle-service-tests` module
1. Run `mvn allure:serve`