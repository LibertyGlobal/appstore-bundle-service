/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2023 Liberty Global Technology Services BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lgi.appstorebundle.test.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;
import java.sql.SQLException;

import static com.lgi.appstorebundle.jooq.generated.AppstoreBundleService.APPSTORE_BUNDLE_SERVICE;

@Slf4j
public abstract class BaseContainersIT {

    protected static final PostgreSQLContainer<?> postgres;
    protected static final RabbitMQContainer rabbitMQ;
    protected static final DSLContext dslContext;
    protected static final WireMockServer wireMockServer;

    static {
        log.info("Starting Postgres database container");
        postgres = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:15")
                        .asCompatibleSubstituteFor("postgres"))
                .withReuse(true);
        postgres.start();

        log.info("Starting database migration");
        Flyway.configure()
                .schemas(APPSTORE_BUNDLE_SERVICE.getName())
                .defaultSchema(APPSTORE_BUNDLE_SERVICE.getName())
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("migration")
                .load()
                .migrate();

        log.info("Creating DSL context");
        try {
            dslContext = DSL.using(DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()), SQLDialect.POSTGRES);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //custom composite array type qualification error workaround - https://github.com/jOOQ/jOOQ/issues/5571
        dslContext.query("ALTER ROLE " + postgres.getUsername() + " SET search_path TO " + APPSTORE_BUNDLE_SERVICE.getName()).execute();
        dslContext.query("ALTER DATABASE " + postgres.getDatabaseName() + " SET search_path TO " + APPSTORE_BUNDLE_SERVICE.getName()).execute();

        log.info("Starting RabbitMQ container");
        rabbitMQ = new RabbitMQContainer("rabbitmq:3.8.19-management-alpine")
                .withReuse(true)
                .withQueue("bundlegen-service-requests")
                .withQueue("bundlegen-service-status")
                .withQueue("bundlecrypt-service-requests")
                .withQueue("bundlecrypt-service-status");
        rabbitMQ.start();

        log.info("Starting WireMock server");
        wireMockServer = new WireMockServer(Options.DYNAMIC_PORT);
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("database.port", postgres::getFirstMappedPort);
        registry.add("database.name", postgres::getDatabaseName);
        registry.add("spring.datasource.hikari.read.username", postgres::getUsername);
        registry.add("spring.datasource.hikari.read.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.write.username", postgres::getUsername);
        registry.add("spring.datasource.hikari.write.password", postgres::getPassword);
        registry.add("rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("asms.service.url", () -> String.format("http://localhost:%s", wireMockServer.port()));
    }
}
