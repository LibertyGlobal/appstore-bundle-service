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
package com.lgi.appstorebundle.external.asms.configuration;

import autovalue.shaded.org.jetbrains.annotations.NotNull;
import com.lgi.appstorebundle.common.r4j.configuration.BulkheadConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class AsmsBulkheadConfiguration implements BulkheadConfiguration {

    @Value("${asms.r4j.bh.name}")
    @NotNull
    private String name;

    @Value("${asms.r4j.bh.maxConcurrentCalls}")
    @NotNull
    private Integer maxConcurrentCalls;

    @Value("${asms.r4j.bh.maxWaitDuration}")
    @NotNull
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration maxWaitDuration;

    @Value("${asms.r4j.bh.writableStackTraceEnabled}")
    private Boolean writableStackTraceEnabled;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    @Override
    public Duration getMaxWaitDuration() {
        return maxWaitDuration;
    }

    @Override
    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled == null || writableStackTraceEnabled;
    }
}
