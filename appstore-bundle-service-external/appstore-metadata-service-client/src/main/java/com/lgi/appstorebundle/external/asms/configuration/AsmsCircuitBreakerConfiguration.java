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
import com.lgi.appstorebundle.common.r4j.configuration.CircuitBreakerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AsmsCircuitBreakerConfiguration implements CircuitBreakerConfiguration {

    @Value("${asms.r4j.cb.name}")
    @NotNull
    private String name;

    @Value("${asms.r4j.cb.failure_rate_threshold}")
    @NotNull
    private Float failureRateThreshold;

    @Value("${asms.r4j.cb.wait_duration_in_open_state}")
    @NotNull
    private Duration waitDurationInOpenState;

    @Value("${asms.r4j.cb.ring_buffer_size_in_half_open_state}")
    @NotNull
    private Integer ringBufferSizeInHalfOpenState;

    @Value("${asms.r4j.cb.ring_buffer_size_in_closed_state}")
    @NotNull
    private Integer ringBufferSizeInClosedState;

    @Value("${asms.r4j.cb.is_automatic_transition_from_open_to_half_open_enabled}")
    @NotNull
    private Boolean isAutomaticTransitionFromOpenToHalfOpenEnabled;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    @Override
    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    @Override
    public int getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    @Override
    public int getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    @Override
    public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() {
        return isAutomaticTransitionFromOpenToHalfOpenEnabled;
    }
}
