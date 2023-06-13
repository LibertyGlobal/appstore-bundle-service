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
package com.lgi.appstorebundle;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.lgi.appstorebundle.resources.AppStoreBundleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

@SpringBootTest
@ContextConfiguration(initializers = AppStoreBundleConfigurationTest.Initializer.class)
@TestPropertySource(locations="classpath:unit-tests.properties")
@ActiveProfiles("dev")
class AppStoreBundleConfigurationTest {

    @Autowired
    private AppStoreBundleController bundleResource;

    @Test
    void sanityCheck() {
        assertThat(bundleResource).isNotNull();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private static final String RABBIT_MQ_IMAGE = "rabbitmq:3.8.19-management-alpine";
        private static final Integer RABBIT_MQ_PORT = 5672;

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            createAndRunPostgresContainer();
            createAndRunRabbitMqContainer();
        }

        private void createAndRunPostgresContainer() {
            var postgres = new PostgreSQLContainer<>("postgres:12");
            postgres.withDatabaseName("postgres");
            postgres.setHostAccessible(true);
            postgres.withExposedPorts(POSTGRESQL_PORT);
            postgres.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(
                            Ports.Binding.bindPort(5433),
                            new ExposedPort(POSTGRESQL_PORT))
                    )
            ));
            postgres.withUsername("postgres");
            postgres.withPassword("postgres");
            postgres.start();
            postgres.getExposedPorts();
            postgres.getPortBindings();
        }

        public void createAndRunRabbitMqContainer() {
            final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(RABBIT_MQ_IMAGE);
            rabbitMQContainer.withQueue("bundlegen-service-requests")
                    .withQueue("bundlegen-service-status")
                    .withQueue("bundlecrypt-service-requests")
                    .withQueue("bundlecrypt-service-status");
            rabbitMQContainer
                    .withExposedPorts(RABBIT_MQ_PORT)
                    .withCreateContainerCmdModifier(cmd -> cmd
                            .withPortBindings(new PortBinding(Ports.Binding.bindPort(RABBIT_MQ_PORT),
                                    new ExposedPort(RABBIT_MQ_PORT))));
            rabbitMQContainer.start();
        }
    }

}