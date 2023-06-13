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
package com.lgi.appstorebundle.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.lgi.appstorebundle.api.Environment;
import com.lgi.appstorebundle.common.r4j.AsmsClientInvoker;
import com.lgi.appstorebundle.common.r4j.ClientInvokerFactory;
import com.lgi.appstorebundle.external.asms.AppstoreMetadataServiceClient;
import com.lgi.appstorebundle.external.asms.configuration.AsmsBulkheadConfiguration;
import com.lgi.appstorebundle.external.asms.configuration.AsmsCircuitBreakerConfiguration;
import com.lgi.appstorebundle.model.EncryptionMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
public class AppStoreBundleServiceConfiguration {

    @Autowired
    private AsmsCircuitBreakerConfiguration asmsCircuitBreakerConfiguration;

    @Autowired
    private AsmsBulkheadConfiguration asmsBulkheadConfiguration;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JodaModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public AsmsClientInvoker asmsClientInvoker() {
        return ClientInvokerFactory.createClientInvoker(
                asmsCircuitBreakerConfiguration,
                asmsBulkheadConfiguration,
                AppstoreMetadataServiceClient.class
        );
    }

    @Bean
    public EncryptionMessageFactory encryptionMessageFactory(@Value("${environment}") Environment environment,
                                                             @Value("${bundle.extension}") @NotNull String bundleExtension) {
        return new EncryptionMessageFactory(environment, bundleExtension);
    }

}