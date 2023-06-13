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

import com.lgi.appstorebundle.common.r4j.exception.RecoverableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ClientInvokerTest {

    private static final int CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD = 2;

    private static final int MAX_CONCURRENT_CALLS = 2;
    private static final Duration MAX_WAIT_DURATION = Duration.ofMillis(100);

    private AsmsClientInvoker clientInvoker;
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        circuitBreaker = createCircuitBreaker();
        bulkhead = createBulkhead(MAX_CONCURRENT_CALLS, MAX_WAIT_DURATION);
        clientInvoker = new AsmsClientInvoker(circuitBreaker, bulkhead);
    }

    @Test
    void circuitOpensOnExceptionWhenInvoke() {
        //GIVEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        //WHEN
        runDecoratedTask(() -> clientInvoker.invoke(this::throwRuntimeException));
        //THEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void callRejectedIfBulkheadFullWhenInvoke() {
        //GIVEN
        Bulkhead fullBulkhead = createBulkhead(0, Duration.ZERO);
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, fullBulkhead);
        //WHEN, THEN
        assertThatExceptionOfType(BulkheadFullException.class)
                .isThrownBy(() -> clientInvoker.invoke(() -> null));
    }

    @Test
    void circuitOpensOnExceptionWhenInvokeWithFallback() {
        //GIVEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        //WHEN
        runDecoratedTask(() -> clientInvoker.invoke(this::throwRuntimeException, ex -> "fallback"));
        //THEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void fallbackAppliedIfBulkheadFullWhenInvoke() {
        //GIVEN
        Bulkhead fullBulkhead = createBulkhead(0, Duration.ZERO);
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, fullBulkhead);
        //WHEN, THEN
        String result = clientInvoker.invoke(() -> null, ex -> "fallback");
        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void circuitOpensOnExceptionWhenInvokeAsync() {
        //GIVEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        //WHEN
        runDecoratedTask(() -> clientInvoker.invokeCompletionStage(() -> CompletableFuture.failedFuture(new RuntimeException())));
        //THEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void callRejectedIfBulkheadFullWhenInvokeAsync() {
        //GIVEN
        Bulkhead fullBulkhead = createBulkhead(0, Duration.ZERO);
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, fullBulkhead);
        //WHEN, THEN
        assertThat(clientInvoker.invokeCompletionStage(() -> CompletableFuture.completedFuture("result")))
                .hasFailedWithThrowableThat()
                .isInstanceOf(BulkheadFullException.class);
    }

    @Test
    void circuitOpensOnExceptionWhenRunAsync() {
        //GIVEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        //WHEN
        runDecoratedTask(() -> clientInvoker.invoke(this::throwRuntimeException));
        //THEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitClosesOnSuccessfulCallsWhenRunAsync() {
        //GIVEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        runDecoratedTask(() -> clientInvoker.invoke(this::throwRuntimeException));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        await().atMost(300L, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN));
        //WHEN
        runDecoratedTask(() -> clientInvoker.invoke(() -> ""));
        //THEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void callRejectedIfBulkheadFullWhenInvokeRunnable() {
        //GIVEN
        Bulkhead fullBulkhead = createBulkhead(0, Duration.ZERO);
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, fullBulkhead);
        //WHEN, THEN
        assertThatExceptionOfType(BulkheadFullException.class)
                .isThrownBy(() -> clientInvoker.invoke(() -> {}));
    }

    @Test
    void runsCircuitBreakerOnlyIfNullBulkheadWhenInvoke() {
        //GIVEN
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, null);
        //WHEN, THEN
        String result = assertDoesNotThrow(() -> clientInvoker.invoke(() -> "result"));
        assertThat(result).isEqualTo("result");
    }

    @Test
    void runsCircuitBreakerOnlyIfNullBulkheadWhenInvokeCompletionStage() {
        //GIVEN
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, null);
        //WHEN, THEN
        CompletableFuture<String> result = assertDoesNotThrow(() -> clientInvoker.invokeCompletionStage(() -> CompletableFuture.completedFuture("result")));
        assertThat(result).isCompleted();
    }

    @Test
    void runsCircuitBreakerOnlyIfNullBulkheadWhenInvokeRunnable() {
        //GIVEN
        CircuitBreaker circuitBreaker = createCircuitBreaker();
        clientInvoker = new AsmsClientInvoker(circuitBreaker, null);
        //WHEN, THEN
        assertDoesNotThrow(() -> clientInvoker.invoke(() -> {}));
    }


    private void runDecoratedTask(Runnable task) {
        for (int i = 0; i < CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD; i++) {
            try {
                task.run();
            } catch (Exception ex) {
                //consumed
            }
        }
    }

    private String throwRuntimeException() {
        throw new RuntimeException();
    }

    private CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(100)
                .ringBufferSizeInClosedState(CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD)
                .ringBufferSizeInHalfOpenState(CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD)
                .ignoreException(RecoverableException.class::isInstance)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        return CircuitBreaker.of("test circuit breaker name", circuitBreakerConfig);
    }

    private Bulkhead createBulkhead(int maxConcurrentCalls, Duration maxWaitDuration) {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(maxWaitDuration)
                .build();
        return Bulkhead.of("test bulkhead", bulkheadConfig);
    }
}