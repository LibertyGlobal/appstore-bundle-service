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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.SupplierUtils;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AsmsClientInvoker {

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public AsmsClientInvoker(CircuitBreaker circuitBreaker, @Nullable Bulkhead bulkhead) {
        this.circuitBreaker = requireNonNull(circuitBreaker, "circuitBreaker");
        this.bulkhead = bulkhead;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public <T> T invoke(Supplier<T> task) {
        return decorateSupplier(task).get();
    }

    public <T> T invoke(Supplier<T> task, Function<Throwable, T> fallback) {
        final Supplier<T> decorated = decorateSupplier(task);

        return SupplierUtils.recover(decorated, fallback).get();
    }

    public void invoke(Runnable task) {
        decorateRunnable(task).run();
    }

    public <T> CompletableFuture<T> invokeCompletionStage(Supplier<CompletionStage<T>> task) {
        return decorateCompletionStage(task).get()
                .toCompletableFuture();
    }

    public <T> Supplier<T> decorateSupplier(Supplier<T> task) {
        Supplier<T> circuitBreakerDecorated = CircuitBreaker.decorateSupplier(circuitBreaker, task);
        return Optional.ofNullable(bulkhead)
                .map(bh -> Bulkhead.decorateSupplier(bh, circuitBreakerDecorated))
                .orElse(circuitBreakerDecorated);
    }

    public <T> Supplier<CompletionStage<T>> decorateCompletionStage(Supplier<CompletionStage<T>> task) {
        Supplier<CompletionStage<T>> circuitBreakerDecorated = CircuitBreaker.decorateCompletionStage(circuitBreaker, task);
        return Optional.ofNullable(bulkhead)
                .map(bh -> Bulkhead.decorateCompletionStage(bh, circuitBreakerDecorated))
                .orElse(circuitBreakerDecorated);
    }

    public Runnable decorateRunnable(Runnable task) {
        Runnable circuitBreakerDecorated = CircuitBreaker.decorateRunnable(circuitBreaker, task);
        return Optional.ofNullable(bulkhead)
                .map(bh -> Bulkhead.decorateRunnable(bh, circuitBreakerDecorated))
                .orElse(circuitBreakerDecorated);
    }
}
