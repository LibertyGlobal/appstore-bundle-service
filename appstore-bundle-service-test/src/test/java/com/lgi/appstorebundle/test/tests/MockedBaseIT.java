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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstorebundle.api.Environment;
import com.lgi.appstorebundle.test.service.endpoints.StartBundleGenerationEndpoint;
import com.lgi.appstorebundle.test.utils.ASMSMockSteps;
import com.lgi.appstorebundle.test.utils.DatabaseSteps;
import com.lgi.appstorebundle.test.utils.RabbitMQSteps;
import com.lgi.appstorebundle.test.utils.TestLifecycleLogger;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ExtendWith(SoftAssertionsExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class MockedBaseIT extends BaseContainersIT implements TestLifecycleLogger {

    protected static DatabaseSteps databaseSteps;
    protected static RabbitMQSteps rabbitMQSteps;
    protected static ASMSMockSteps asmsMockSteps;
    private static boolean stepsInitialized = false;
    @Autowired
    protected StartBundleGenerationEndpoint startBundleGenerationEndpoint;
    @Autowired
    private ObjectMapper jsonMapper;
    @Value("${environment}")
    private Environment environment;

    @BeforeAll
    void beforeAll() throws URISyntaxException, NoSuchAlgorithmException, IOException, KeyManagementException, TimeoutException {
        if (!stepsInitialized) {
            databaseSteps = new DatabaseSteps(dslContext);
            rabbitMQSteps = new RabbitMQSteps(rabbitMQ, jsonMapper, environment);
            asmsMockSteps = new ASMSMockSteps(wireMockServer, jsonMapper);
            stepsInitialized = true;
        }
    }
}
