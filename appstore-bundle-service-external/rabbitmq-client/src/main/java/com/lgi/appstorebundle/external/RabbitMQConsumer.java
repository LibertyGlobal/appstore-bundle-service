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

import com.rabbitmq.client.DeliverCallback;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class RabbitMQConsumer<C> {

    private final Function<C, String> queueNameSupplier;
    private final Supplier<DeliverCallback> consumerSupplier;

    public RabbitMQConsumer(Function<C, String> queueNameSupplier, Supplier<DeliverCallback> consumerSupplier) {
        this.queueNameSupplier = requireNonNull(queueNameSupplier);
        this.consumerSupplier = requireNonNull(consumerSupplier);
    }

    public Function<C, String> getQueueNameSupplier() {
        return queueNameSupplier;
    }

    public DeliverCallback getConsumer() {
        return consumerSupplier.get();
    }

}
