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
package com.lgi.appstorebundle.test.tests;

import com.lgi.appstorebundle.test.service.endpoints.InfoEndpoint;
import com.lgi.appstorebundle.test.service.model.InfoResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.http.HttpStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Epic("Mocked application")
@Feature("Get info")
@Story("Get info")
public class GetInfoIT extends MockedBaseIT {

    @Autowired
    @Qualifier("testInfoEndpoint")
    private InfoEndpoint infoEndpoint;

    @Test
    @Disabled("SPARK-51317") // TODO unignore this test when ASBS infrastructure is updated
    @DisplayName("Call service info - verify successful response")
    void callServiceInfo_verifySuccessfulResponse(SoftAssertions softly) {
        // Given
        var expectedAppName = "appstore-bundle-service-application";

        // When
        var response = infoEndpoint.getInfo();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service")
                .isEqualTo(HttpStatus.SC_OK);
        softly.assertThat(response.as(InfoResponse.class).appName())
                .as("Wrong app name received from the service info")
                .isEqualTo(expectedAppName);
    }
}