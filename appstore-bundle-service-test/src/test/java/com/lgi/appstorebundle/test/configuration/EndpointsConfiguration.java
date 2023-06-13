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
package com.lgi.appstorebundle.test.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstorebundle.test.core.ServiceUrlProvider;
import com.lgi.appstorebundle.test.service.endpoints.InfoEndpoint;
import com.lgi.appstorebundle.test.service.endpoints.StartBundleGenerationEndpoint;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EndpointsConfiguration {

    @Bean
    public RequestSpecification requestSpecification(ObjectMapper jsonMapper) {
        return new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .addFilter(new AllureRestAssured()
                        .setRequestTemplate("request.ftl")
                        .setResponseTemplate("response.ftl"))
                .setConfig(RestAssuredConfig.config()
                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((type, s) -> jsonMapper)))
                .build();
    }

    @Bean(name = "testInfoEndpoint")
    public InfoEndpoint infoEndpoint(
            ServiceUrlProvider serviceUrlProvider,
            RequestSpecification requestSpecification) {
        return new InfoEndpoint(serviceUrlProvider, requestSpecification);
    }

    @Bean
    public StartBundleGenerationEndpoint startBundleGenerationEndpoint(
            ServiceUrlProvider serviceUrlProvider,
            RequestSpecification requestSpecification) {
        return new StartBundleGenerationEndpoint(serviceUrlProvider, requestSpecification);
    }
}
