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
package com.lgi.appstorebundle.common.r4j;

import com.lgi.appstorebundle.common.r4j.configuration.BulkheadConfiguration;
import com.lgi.appstorebundle.common.r4j.configuration.CircuitBreakerConfiguration;
import com.lgi.appstorebundle.common.r4j.exception.RecoverableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientInvokerFactory {

    private ClientInvokerFactory() {
    }

    public static AsmsClientInvoker createClientInvoker(CircuitBreakerConfiguration circuitBreakerConfig,
                                                        Class<?> clazz) {
        CircuitBreaker circuitBreaker = createCircuitBreaker(circuitBreakerConfig, LoggerFactory.getLogger(clazz));
        return new AsmsClientInvoker(circuitBreaker, null);
    }

    public static AsmsClientInvoker createClientInvoker(CircuitBreakerConfiguration circuitBreakerConfig,
                                                        BulkheadConfiguration bulkheadConfiguration, Class<?> clazz) {
        CircuitBreaker circuitBreaker = createCircuitBreaker(circuitBreakerConfig, LoggerFactory.getLogger(clazz));
        Bulkhead bulkhead = createBulkhead(bulkheadConfiguration, LoggerFactory.getLogger(clazz));
        return new AsmsClientInvoker(circuitBreaker, bulkhead);
    }

    private static Bulkhead createBulkhead(BulkheadConfiguration bulkheadConfiguration, Logger logger) {
        Bulkhead bulkhead = BulkheadFactory.create(bulkheadConfiguration);
        bulkhead.getEventPublisher()
                .onCallRejected(event -> logger.warn("Bulkhead execution rejected: name={}", bulkhead.getName()));
        return bulkhead;
    }

    private static CircuitBreaker createCircuitBreaker(CircuitBreakerConfiguration circuitBreakerConfig, Logger logger) {
        CircuitBreaker circuitBreaker = CircuitBreakerFactory.create(circuitBreakerConfig, builder -> builder.ignoreException(RecoverableException.class::isInstance));
        circuitBreaker.getEventPublisher()
                .onIgnoredError(event -> logger.warn("CircuitBreaker ignored exception occurred: name={}, message={}", circuitBreaker.getName(), event.getThrowable().getMessage()))
                .onError(event -> logger.warn("CircuitBreaker execution failed: name={}", circuitBreaker.getName(), event.getThrowable()))
                .onSuccess(event -> logger.debug("CircuitBreaker execution succeeded: name={}", circuitBreaker.getName()))
                .onCallNotPermitted(event -> logger.debug("CircuitBreaker call is not permitted: name={}", circuitBreaker.getName()))
                .onStateTransition(event ->
                        logger.info("CircuitBreaker state transition: name={}, transition={}", circuitBreaker.getName(), event.getStateTransition()));
        return circuitBreaker;
    }
}
