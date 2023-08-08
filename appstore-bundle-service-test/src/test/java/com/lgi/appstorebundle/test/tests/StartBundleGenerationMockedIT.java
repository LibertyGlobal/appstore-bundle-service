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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.model.GenerationMessage;
import com.lgi.appstorebundle.test.service.model.BundleWithAudit;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.apache.http.HttpStatus;
import org.assertj.core.api.SoftAssertions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.lgi.appstorebundle.api.model.BundleStatus.BUNDLE_ERROR;
import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_REQUESTED;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.fail;

@Epic("Mocked application")
@Feature("Start bundle generation")
@Story("Start bundle generation")
public class StartBundleGenerationMockedIT extends MockedBaseIT {

    private static final UUID DUMMY_ID = randomUUID();
    private static final String APP_ID = "APP_ID";
    private static final String APP_VER = "APP_VER";
    private static final String PLATFORM_NAME = "PLATFORM_NAME";
    private static final String FIRMWARE_VER = "FIRMWARE_VER";
    private static final String OCI_IMAGE_URL = "OCI_IMAGE_URL";
    private static final boolean ENCRYPTION_ENABLED = true;
    private static final boolean ENCRYPTION_DISABLED = false;
    private static final String X_REQUEST_ID = "1111-1111111-1111";
    private static final String SECOND_X_REQUEST_ID = "2222-22222222-2222";
    private static final String APP_BUNDLE_NAME = "APP_BUNDLE_NAME";
    private static final String MAINTAINER_CODE = "MAINTAINER_CODE";

    private static final String APP_NAME = "APP_NAME";
    private static final String APP_URL = "http://hostname/demo.id.appl/2.2/platformName/firmwareVer/demo.id.appl_2.2_platformName_firmwareVer.tar.gz";

    private static final ApplicationParams VALID_APP_PARAMS =
            ApplicationParams.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME);

    @BeforeEach
    void setUp() {
        databaseSteps.clearBundleTable();
        rabbitMQSteps.clearStack();
        asmsMockSteps.resetAll();
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 404 because of missing application metadata from ASMS")
    void callRequestBundleWhenAppNotExistsInASMS_verifyNotFoundResponse(SoftAssertions softly) {
        // Given
        asmsMockSteps.stubASMSWithNotFoundForApplicationMetadata(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
        databaseSteps.verifyNotStoredBundle(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER);
        rabbitMQSteps.verifyNotSentGenerationMessage(softly);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 404 because of missing application metadata for maintainer from ASMS")
    void callRequestBundleWhenAppForMaintainerNotExistsInASMS_verifyNotFoundResponse(SoftAssertions softly) throws JsonProcessingException {
        // Given
        asmsMockSteps.stubASMSWithApplicationMetadataInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL);
        asmsMockSteps.stubASMSWithNotFoundForApplicationMetadataForMaintainer(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
        databaseSteps.verifyNotStoredBundle(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER);
        rabbitMQSteps.verifyNotSentGenerationMessage(softly);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 202, bundle stored in DB and message sent. Maintainer sets field encryption = true.")
    void callRequestBundleWhenGenerationNotTriggered_verifyStartingGenerationWithSendingMessageAndSavingToDB_maintainerSetsFieldEncryptionTrue(SoftAssertions softly) throws JsonProcessingException {
        // Given
        asmsMockSteps.stubASMSWithApplicationMetadataInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL);
        asmsMockSteps.stubASMSWithApplicationMetadataForMaintainerInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL, ENCRYPTION_ENABLED, OCI_IMAGE_URL);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_ACCEPTED);
        databaseSteps.verifyBundleWasCreated(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, GENERATION_REQUESTED, X_REQUEST_ID);
        final GenerationMessage expectedGenerationMessage = GenerationMessage.create(DUMMY_ID, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, OCI_IMAGE_URL, ENCRYPTION_ENABLED);
        rabbitMQSteps.verifySentGenerationMessage(softly, expectedGenerationMessage, X_REQUEST_ID);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 202, bundle stored in DB and message sent. Maintainer has field encryption = false.")
    void callRequestBundleWhenGenerationNotTriggered_verifyStartingGenerationWithSendingMessageAndSavingToDB_maintainerSetsFieldEncryptionFalse(SoftAssertions softly) throws JsonProcessingException {
        // Given
        asmsMockSteps.stubASMSWithApplicationMetadataInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL);
        asmsMockSteps.stubASMSWithApplicationMetadataForMaintainerInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL, ENCRYPTION_DISABLED, OCI_IMAGE_URL);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_ACCEPTED);
        databaseSteps.verifyBundleWasCreated(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, GENERATION_REQUESTED, X_REQUEST_ID);
        final GenerationMessage expectedGenerationMessage = GenerationMessage.create(DUMMY_ID, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, OCI_IMAGE_URL, ENCRYPTION_DISABLED);
        rabbitMQSteps.verifySentGenerationMessage(softly, expectedGenerationMessage, X_REQUEST_ID);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 202, started a new bundle generation due to a previous error")
    void callRequestBundleWhenGenerationHadError_verifyStartingNewGenerationWithSendingMessageAndSavingToDB(SoftAssertions softly) throws JsonProcessingException {
        // Given
        final UUID id = randomUUID();
        final DateTime now = now(DateTimeZone.UTC);
        final DateTime messageTimestampForOldRow = now.minusMinutes(2);
        final DateTime createdAtForOldRow = now.minusMinutes(3);
        final DateTime updatedAtForOldRow = now.minusMinutes(1);
        final BundleWithAudit bundleWithAudit = BundleWithAudit.create(
                Bundle.create(
                        id,
                        ApplicationContext.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER),
                        BUNDLE_ERROR,
                        X_REQUEST_ID,
                        messageTimestampForOldRow),
                createdAtForOldRow,
                updatedAtForOldRow);
        databaseSteps.saveBundleWithStatus(bundleWithAudit);
        asmsMockSteps.stubASMSWithApplicationMetadataInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL);
        asmsMockSteps.stubASMSWithApplicationMetadataForMaintainerInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL, ENCRYPTION_ENABLED, OCI_IMAGE_URL);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, SECOND_X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_ACCEPTED);
        databaseSteps.verifySecondBundleWasAdded(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, GENERATION_REQUESTED, SECOND_X_REQUEST_ID, messageTimestampForOldRow, updatedAtForOldRow);
        final GenerationMessage expectedGenerationMessage = GenerationMessage.create(DUMMY_ID, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, OCI_IMAGE_URL, ENCRYPTION_ENABLED);
        rabbitMQSteps.verifySentGenerationMessage(softly, expectedGenerationMessage, SECOND_X_REQUEST_ID);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("GET: Call startBundleGeneration - Status code 202, start a new generation skipped due to existing one")
    void callRequestBundleWhenGenerationWasAlreadyTriggered_verifyNotSendingMessageAndNotSavingToDB(SoftAssertions softly) throws JsonProcessingException {
        // Given
        final UUID id = randomUUID();
        final DateTime now = now(DateTimeZone.UTC);
        final DateTime messageTimestampForOldRow = now.minusMinutes(3);
        final DateTime createdAtForOldRow = now.minusMinutes(2);
        final BundleWithAudit bundleWithAudit = BundleWithAudit.create(
                Bundle.create(
                        id,
                        ApplicationContext.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER),
                        GENERATION_REQUESTED,
                        X_REQUEST_ID,
                        messageTimestampForOldRow),
                createdAtForOldRow,
                null);
        databaseSteps.saveBundleWithStatus(bundleWithAudit);
        asmsMockSteps.stubASMSWithApplicationMetadataInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL);
        asmsMockSteps.stubASMSWithApplicationMetadataForMaintainerInResponseBody(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, MAINTAINER_CODE, APP_NAME, APP_URL, ENCRYPTION_ENABLED, OCI_IMAGE_URL);

        // When
        var response = startBundleGenerationEndpoint.startBundleGeneration(VALID_APP_PARAMS, SECOND_X_REQUEST_ID);
        waitDelay();

        // Then
        softly.assertThat(response.getStatusCode())
                .as("Wrong HTTP status code received from the service.")
                .isEqualTo(HttpStatus.SC_ACCEPTED);
        databaseSteps.verifySecondBundleWasNotAdded(softly, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER);
        rabbitMQSteps.verifyNotSentGenerationMessage(softly);
    }

    private void waitDelay() {
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            fail(e);
        }
    }
}