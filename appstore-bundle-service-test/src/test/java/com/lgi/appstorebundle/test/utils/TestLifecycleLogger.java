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
package com.lgi.appstorebundle.test.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@TestInstance(Lifecycle.PER_CLASS)
public interface TestLifecycleLogger {
    Logger log = LoggerFactory.getLogger(TestLifecycleLogger.class);

    @BeforeAll
    default void logStartTestClass(TestInfo testInfo) {
        String className = testInfo.getTestClass().map(Class::getSimpleName).orElse("");
        log.info("Test class started: {}", className);
    }

    @AfterAll
    default void logEndTestClass(TestInfo testInfo) {
        String className = testInfo.getTestClass().map(Class::getSimpleName).orElse("");
        log.info("Test class finished: {}", className);
    }

    @BeforeEach
    default void logStartTest(TestInfo testInfo) {
        log.info("Test started: [{}] :: {}", testInfo.getTestMethod().map(Method::getName).orElse(""), testInfo.getDisplayName());
    }

    @AfterEach
    default void logEndTest(TestInfo testInfo) {
        log.info("Test finished: [{}] :: {}", testInfo.getTestMethod().map(Method::getName).orElse(""), testInfo.getDisplayName());
    }
}
