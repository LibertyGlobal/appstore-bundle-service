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
import com.lgi.appstorebundle.common.r4j.exception.DefaultRecoverableUpstreamServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientInvokerFactoryTest {

    private static final int RING_BUFFER_SIZE = 2;
    private static final boolean WRITABLE_STACK_TRACE_ENABLED = true;

    @Test
    void givenCircuitBreakerConfigWhenCreateInvokerThenCreated() {
        //GIVEN, WHEN
        AsmsClientInvoker invoker = ClientInvokerFactory.createClientInvoker(testCircuitBreaker(), ClientInvokerFactoryTest.class);
        //THEN
        assertNotNull(invoker);
    }

    @Test
    void givenCircuitBreakerConfigWhenRecoverableExceptionThenIgnored() {
        //GIVEN
        AsmsClientInvoker invoker = ClientInvokerFactory.createClientInvoker(testCircuitBreaker(), ClientInvokerFactoryTest.class);
        //WHEN
        for (int i = 0; i < RING_BUFFER_SIZE + 1; i++) {
            invoker.invoke(() -> {
                throw new DefaultRecoverableUpstreamServiceException("error");
            }, Function.identity());
        }
        //THEN
        assertEquals(CircuitBreaker.State.CLOSED, invoker.getCircuitBreaker().getState());
    }

    @Test
    void givenCircuitBreakerConfigWithBulkheadWhenCreateInvokerThenCreated() {
        //GIVEN, WHEN
        AsmsClientInvoker invoker = ClientInvokerFactory.createClientInvoker(testCircuitBreaker(), testBulkhead(), ClientInvokerFactoryTest.class);
        //THEN
        assertNotNull(invoker);
    }

    private BulkheadConfiguration testBulkhead() {
        BulkheadConfiguration bulkheadConfigurationMock = mock(BulkheadConfiguration.class);
        when(bulkheadConfigurationMock.getName()).thenReturn("bh");
        when(bulkheadConfigurationMock.getMaxConcurrentCalls()).thenReturn(2);
        when(bulkheadConfigurationMock.getMaxWaitDuration()).thenReturn(Duration.ofMillis(500));
        when(bulkheadConfigurationMock.isWritableStackTraceEnabled()).thenReturn(WRITABLE_STACK_TRACE_ENABLED);
        return bulkheadConfigurationMock;
    }

    private CircuitBreakerConfiguration testCircuitBreaker() {
        CircuitBreakerConfiguration cbMock = mock(CircuitBreakerConfiguration.class);
        when(cbMock.getName()).thenReturn("cb");
        when(cbMock.getFailureRateThreshold()).thenReturn( 100f);
        when(cbMock.getWaitDurationInOpenState()).thenReturn(Duration.ofMillis(500));
        when(cbMock.getRingBufferSizeInHalfOpenState()).thenReturn(RING_BUFFER_SIZE);
        when(cbMock.getRingBufferSizeInClosedState()).thenReturn(RING_BUFFER_SIZE);
        when(cbMock.isAutomaticTransitionFromOpenToHalfOpenEnabled()).thenReturn(true);
        return cbMock;
    }

}