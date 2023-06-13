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

import java.util.Objects;
import java.util.function.Consumer;

public final class OptionalException {

    private static final OptionalException EMPTY = new OptionalException();

    private final RuntimeException value;

    public static OptionalException empty() {
        return EMPTY;
    }

    public static OptionalException of(RuntimeException value) {
        return new OptionalException(value);
    }

    private OptionalException() {
        this.value = null;
    }

    private OptionalException(RuntimeException value) {
        this.value = Objects.requireNonNull(value);
    }

    public void ifPresent(Consumer<RuntimeException> action) {
        if (value != null) {
            action.accept(value);
        }
    }
}
