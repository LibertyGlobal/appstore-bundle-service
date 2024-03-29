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
package com.lgi.appstorebundle.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
public class RabbitMQConfiguration {

    @Value("${rabbitmq.generationQueueName}")
    @NotNull
    private String generationQueueName;

    @Value("${rabbitmq.generationStatusQueueName}")
    @NotNull
    private String generationStatusQueueName;

    @Value("${rabbitmq.encryptionQueueName}")
    @NotNull
    private String encryptionQueueName;

    @Value("${rabbitmq.encryptionStatusQueueName}")
    @NotNull
    private String encryptionStatusQueueName;

    @Value("${rabbitmq.url}")
    @NotNull
    private String url;

    public String getGenerationQueueName() {
        return generationQueueName;
    }

    public String getGenerationStatusQueueName() {
        return generationStatusQueueName;
    }

    public String getEncryptionQueueName() {
        return encryptionQueueName;
    }

    public String getEncryptionStatusQueueName() {
        return encryptionStatusQueueName;
    }

    public String getUrl() {
        return url;
    }

}
