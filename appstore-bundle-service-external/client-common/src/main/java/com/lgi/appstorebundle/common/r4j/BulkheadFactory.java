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
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.prometheus.collectors.BulkheadMetricsCollector;
import io.prometheus.client.CollectorRegistry;

public final class BulkheadFactory {

    private static final BulkheadRegistry BULKHEAD_REGISTRY = BulkheadRegistry.ofDefaults();
    private static final BulkheadMetricsCollector BULKHEAD_METRICS_COLLECTOR = BulkheadMetricsCollector.ofBulkheadRegistry(BULKHEAD_REGISTRY);

    static {
        CollectorRegistry.defaultRegistry.register(BULKHEAD_METRICS_COLLECTOR);
    }

    private BulkheadFactory() {
    }

    public static Bulkhead create(BulkheadConfiguration configuration) {
        return create(configuration, CollectorRegistry.defaultRegistry);
    }

    public static Bulkhead create(BulkheadConfiguration configuration, CollectorRegistry collectorRegistry) {
        BulkheadConfig bulkheadConfig = new BulkheadConfig.Builder()
                .maxConcurrentCalls(configuration.getMaxConcurrentCalls())
                .maxWaitDuration(configuration.getMaxWaitDuration())
                .writableStackTraceEnabled(configuration.isWritableStackTraceEnabled())
                .build();

        if (CollectorRegistry.defaultRegistry.equals(collectorRegistry)) {
            return BULKHEAD_REGISTRY.bulkhead(configuration.getName(), bulkheadConfig);
        }

        final BulkheadRegistry bulkheadRegistry = BulkheadRegistry.of(bulkheadConfig);
        collectorRegistry.register(BulkheadMetricsCollector.ofBulkheadRegistry(bulkheadRegistry));

        return bulkheadRegistry.bulkhead(configuration.getName());
    }

}
