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

import com.lgi.appstorebundle.common.r4j.configuration.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector;
import io.prometheus.client.CollectorRegistry;

import java.util.function.Consumer;

public final class CircuitBreakerFactory {

    private static final CircuitBreakerRegistry CIRCUIT_BREAKER_REGISTRY = CircuitBreakerRegistry.ofDefaults();
    private static final CircuitBreakerMetricsCollector CIRCUIT_BREAKER_METRICS_COLLECTOR =
            CircuitBreakerMetricsCollector.ofCircuitBreakerRegistry(CIRCUIT_BREAKER_REGISTRY);

    static {
        CollectorRegistry.defaultRegistry.register(CIRCUIT_BREAKER_METRICS_COLLECTOR);
    }

    private CircuitBreakerFactory() {
    }

    public static CircuitBreaker create(CircuitBreakerConfiguration configuration) {
        return create(configuration, CollectorRegistry.defaultRegistry);
    }

    public static CircuitBreaker create(CircuitBreakerConfiguration configuration, Consumer<CircuitBreakerConfig.Builder> additionalConfig) {
        return create(configuration, CollectorRegistry.defaultRegistry, additionalConfig);
    }

    public static CircuitBreaker create(CircuitBreakerConfiguration configuration, CollectorRegistry metricsRegistry) {
        return create(configuration, metricsRegistry, builder -> {});
    }

    public static CircuitBreaker create(CircuitBreakerConfiguration configuration,
                                        CollectorRegistry metricsRegistry,
                                        Consumer<CircuitBreakerConfig.Builder> additionalConfig) {
        CircuitBreakerConfig.Builder builder = new CircuitBreakerConfig.Builder()
                .failureRateThreshold(configuration.getFailureRateThreshold())
                .waitDurationInOpenState(configuration.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(configuration.getRingBufferSizeInHalfOpenState())
                .slidingWindowSize(configuration.getRingBufferSizeInClosedState())
                .automaticTransitionFromOpenToHalfOpenEnabled(configuration.isAutomaticTransitionFromOpenToHalfOpenEnabled());

        additionalConfig.accept(builder);

        if (CollectorRegistry.defaultRegistry.equals(metricsRegistry)) {
            return CIRCUIT_BREAKER_REGISTRY.circuitBreaker(configuration.getName(), builder.build());
        }

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(builder.build());
        metricsRegistry.register(CircuitBreakerMetricsCollector.ofCircuitBreakerRegistry(cbRegistry));

        return cbRegistry.circuitBreaker(configuration.getName());
    }
}
