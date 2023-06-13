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
package com.lgi.appstorebundle.test.service.endpoints;

import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.test.core.ServiceUrlProvider;
import com.lgi.appstorebundle.test.utils.RabbitMQSteps;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.NonNull;

public class StartBundleGenerationEndpoint extends BaseEndpoint<StartBundleGenerationEndpoint> {
    private static final String REQUEST_BUNDLE_PATH = "/applications/{appId}/{appVersion}/{platformName}/{firmwareVersion}/{appBundleName}";

    public StartBundleGenerationEndpoint(@NonNull ServiceUrlProvider urlProvider,
                                         @NonNull RequestSpecification requestSpecification) {
        super(urlProvider, requestSpecification);
    }

    @Step("Request for starting bundle generation")
    public Response startBundleGeneration(ApplicationParams appParams, String xRequestId) {
        return given()
                .when()
                .pathParam("appId", appParams.getApplicationId())
                .pathParam("appVersion", appParams.getAppVersion())
                .pathParam("platformName", appParams.getPlatformName())
                .pathParam("firmwareVersion", appParams.getFirmwareVersion())
                .pathParam("appBundleName", appParams.getAppBundleName())
                .header(RabbitMQSteps.X_REQUEST_ID, xRequestId)
                .get(REQUEST_BUNDLE_PATH)
                .then()
                .extract()
                .response();
    }
}
