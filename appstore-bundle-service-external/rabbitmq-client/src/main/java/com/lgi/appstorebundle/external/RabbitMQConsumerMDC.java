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

import org.slf4j.MDC;

import static com.lgi.appstorebundle.common.Headers.CORRELATION_ID;

public class RabbitMQConsumerMDC {

    static final String RABBITMQ_MDC_INDICATOR = "rabbitmq-mdc";

    private RabbitMQConsumerMDC() {
        //empty
    }

    public static void populate(String xRequestId) {
        if (MDC.get(RABBITMQ_MDC_INDICATOR) == null) {
            MDC.put(RABBITMQ_MDC_INDICATOR, "true");
            MDC.put(CORRELATION_ID, xRequestId);
        }
    }

    public static void clear() {
        String kafkaMdc = MDC.get(RABBITMQ_MDC_INDICATOR);
        if ("true".equals(kafkaMdc)) {
            MDC.remove(CORRELATION_ID);
            MDC.remove(RABBITMQ_MDC_INDICATOR);
        }
    }
}
