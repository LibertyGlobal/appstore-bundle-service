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
package com.lgi.appstorebundle.test.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Builder
@Accessors(fluent = true)
@JsonInclude(Include.NON_NULL)
public class InfoResponse {

    @JsonProperty("APP_DEPLOY_TIME")
    String appDeployTime;

    @JsonProperty("APP_START_TIME")
    String appStartTime;

    @JsonProperty("HOST_NAME")
    String hostName;

    @JsonProperty("APP_NAME")
    String appName;

    @JsonProperty("APP_BRANCH")
    String appBranch;

    @JsonProperty("APP_BUILD_TIME")
    String appBuildTime;

    @JsonProperty("APP_VERSION")
    String appVersion;

    @JsonProperty("STACK_NAME")
    String stackName;

    @JsonProperty("APP_REVISION")
    String appRevision;
}
