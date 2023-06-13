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
package com.lgi.appstorebundle.api;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ApplicationParams {

    public abstract String getApplicationId();

    public abstract String getAppVersion();

    public abstract String getPlatformName();

    public abstract String getFirmwareVersion();

    public abstract String getAppBundleName();

    public static ApplicationParams create(String appId, String appVer, String platformName, String firmwareVersion, String appBundleName) {
        return new AutoValue_ApplicationParams(appId, appVer, platformName, firmwareVersion, appBundleName);
    }

    public String getFullyQualifiedApplicationName(){
        return getApplicationId() + ":" + getAppVersion();
    }
}
