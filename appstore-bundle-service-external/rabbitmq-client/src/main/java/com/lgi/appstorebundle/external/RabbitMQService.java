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

import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleContext;
import com.lgi.appstorebundle.model.EncryptionMessage;
import com.lgi.appstorebundle.model.GenerationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQService {

    private final ManagedRabbitMQ rabbitMQ;

    @Autowired
    public RabbitMQService(ManagedRabbitMQ rabbitMQ) {
        this.rabbitMQ = rabbitMQ;
    }

    public OptionalException sendGenerationMessage(BundleContext bundleContext) {
        final Bundle bundle = bundleContext.getBundle();
        final ApplicationContext applicationContext = bundle.getApplicationContext();
        GenerationMessage generationMessage = GenerationMessage.create(
                bundle.getId(),
                applicationContext.getApplicationId(),
                applicationContext.getApplicationVersion(),
                applicationContext.getPlatformName(),
                applicationContext.getFirmwareVersion(),
                bundleContext.getOciImageUrl(),
                bundleContext.getEncrypt()
        );
        return rabbitMQ.sendGenerationMessage(generationMessage, bundle.getXRequestId());
    }

    public OptionalException sendEncryptionMessage(EncryptionMessage encryptionMessage, String xRequestId) {
        return rabbitMQ.sendEncryptionMessage(encryptionMessage, xRequestId);
    }
}
