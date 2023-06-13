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
package com.lgi.appstorebundle.error.exception;

public class ApplicationNotFoundException extends RuntimeException {

    public static final String APP_NOT_FOUND = "Application id: '%s'. version: '%s', platformName: '%s', firmwareVersion: '%s' not found in AppStore Metadata Service";

    public ApplicationNotFoundException(String message) {
        super(message);
    }

    public static ApplicationNotFoundException createDefault(String appId, String appVersion, String platformName, String firmwareVersion) {
        return new ApplicationNotFoundException(String.format(APP_NOT_FOUND, appId, appVersion, platformName, firmwareVersion));
    }
}