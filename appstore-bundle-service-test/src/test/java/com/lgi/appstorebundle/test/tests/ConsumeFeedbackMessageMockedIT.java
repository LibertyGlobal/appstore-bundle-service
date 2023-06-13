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

import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.model.EncryptionMessageFactory;
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.lgi.appstorebundle.test.service.model.BundleWithAudit;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.assertj.core.api.SoftAssertions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.lgi.appstorebundle.api.model.BundleStatus.ENCRYPTION_REQUESTED;
import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_COMPLETED;
import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_LAUNCHED;
import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_REQUESTED;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.fail;

@Epic("Mocked application")
@Feature("Consume feedback message")
@Story("Consume feedback message")
public class ConsumeFeedbackMessageMockedIT extends MockedBaseIT {

    private static final String APP_ID = "APP_ID";
    private static final String APP_VER = "APP_VER";
    private static final String PLATFORM_NAME = "PLATFORM_NAME";
    private static final String FIRMWARE_VER = "FIRMWARE_VER";
    private static final String X_REQUEST_ID = "1111-1111111-1111";

    @Autowired
    EncryptionMessageFactory encryptionMessageFactory;

    @BeforeEach
    void setUp() {
        databaseSteps.clearBundleTable();
        rabbitMQSteps.clearStack();

    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("Saving a new status for application when Feedback Message has greater message timestamp")
    void receivedNewerFeedbackMessage_saveNewStatus(SoftAssertions softly) throws IOException {
        // Given
        final UUID id = randomUUID();
        final String xRequestId = "1111-1111111-2222";
        final DateTime now = now(DateTimeZone.UTC);
        final DateTime messageTimestampForOldRow = now.minusMinutes(3);
        final DateTime createdAtForOldRow = now.minusMinutes(2);
        final BundleWithAudit bundleWithAudit = BundleWithAudit.create(
                Bundle.create(
                        id,
                        ApplicationContext.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER),
                        GENERATION_REQUESTED,
                        xRequestId,
                        messageTimestampForOldRow),
                createdAtForOldRow,
                null);
        databaseSteps.saveBundleWithStatus(bundleWithAudit);

        // When
        rabbitMQSteps.pushMessage(FeedbackMessage.create(bundleWithAudit.getBundle().getId(), GENERATION_LAUNCHED.name(), now, null), xRequestId);
        waitDelay();

        // Then
        databaseSteps.verifyBundleWasUpdated(softly, bundleWithAudit.getBundle().getId(), APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, GENERATION_LAUNCHED, xRequestId, messageTimestampForOldRow, createdAtForOldRow, null);
    }

    @Test
    @Issue("SPARK-36509")
    @DisplayName("Sends encryption message when bundle generation is done and encryption is enabled")
    void receivedNewerFeedbackMessage_whenEncryptionEnabledAndBundleStatusIsGenerationCompleted_publishEncryptionMessage(SoftAssertions softly) throws IOException {
        // Given
        final UUID id = randomUUID();
        final DateTime now = now(DateTimeZone.UTC);
        final DateTime messageTimestampForOldRow = now.minusMinutes(3);
        final DateTime createdAtForOldRow = now.minusMinutes(2);
        final DateTime updatedAtForOldRow = now.minusMinutes(1);
        final BundleWithAudit bundleWithAudit = BundleWithAudit.create(
                Bundle.create(
                        id,
                        ApplicationContext.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER),
                        GENERATION_LAUNCHED,
                        X_REQUEST_ID,
                        messageTimestampForOldRow),
                createdAtForOldRow,
                updatedAtForOldRow);
        databaseSteps.saveBundleWithStatus(bundleWithAudit);

        // When
        rabbitMQSteps.pushMessage(FeedbackMessage.create(bundleWithAudit.getBundle().getId(), GENERATION_COMPLETED.name(), now, null), X_REQUEST_ID);
        waitDelay();

        // Then
        rabbitMQSteps.verifySentEncryptionMessage(softly, encryptionMessageFactory.fromBundle(bundleWithAudit.getBundle()), X_REQUEST_ID);
        databaseSteps.verifyBundleWasUpdated(softly, bundleWithAudit.getBundle().getId(), APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, ENCRYPTION_REQUESTED, X_REQUEST_ID, messageTimestampForOldRow, createdAtForOldRow, updatedAtForOldRow);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("Do not saving a new status for application when Feedback Message has lower message timestamp")
    void receivedOlderFeedbackMessage_doNotSaveNewStatus(SoftAssertions softly) throws IOException {
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
                        GENERATION_COMPLETED,
                        X_REQUEST_ID,
                        messageTimestampForOldRow),
                createdAtForOldRow,
                updatedAtForOldRow);
        databaseSteps.saveBundleWithStatus(bundleWithAudit);

        // When
        rabbitMQSteps.pushMessage(FeedbackMessage.create(bundleWithAudit.getBundle().getId(), GENERATION_LAUNCHED.name(), messageTimestampForOldRow.minusMinutes(1), null), X_REQUEST_ID);
        waitDelay();

        // Then
        databaseSteps.verifyBundleWasNotUpdated(softly, bundleWithAudit.getBundle().getId(), GENERATION_COMPLETED, messageTimestampForOldRow, updatedAtForOldRow);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("Do not saving a new status for application when message not contains a x-request-id")
    void receivedFeedbackMessageWithoutXRequestId_doNotSaveNewStatus(SoftAssertions softly) throws IOException {
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

        rabbitMQSteps.pushMessage(FeedbackMessage.create(bundleWithAudit.getBundle().getId(), GENERATION_LAUNCHED.name(), now, null), null);
        waitDelay();

        // Then
        databaseSteps.verifyBundleWasNotUpdated(softly, bundleWithAudit.getBundle().getId(), GENERATION_REQUESTED, messageTimestampForOldRow, null);
    }

    @Test
    @Issue("SPARK-36508")
    @DisplayName("Do not saving a new status for application when message not contains a messageTimestamp")
    void receivedFeedbackMessageWithoutMessageTimestamp_doNotSaveNewStatus(SoftAssertions softly) throws IOException {
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

        rabbitMQSteps.pushMessage(FeedbackMessage.create(bundleWithAudit.getBundle().getId(), GENERATION_LAUNCHED.name(), null, null), X_REQUEST_ID);
        waitDelay();

        // Then
        databaseSteps.verifyBundleWasNotUpdated(softly, bundleWithAudit.getBundle().getId(), GENERATION_REQUESTED, messageTimestampForOldRow, null);
    }

    private void waitDelay() {
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            fail(e);
        }
    }
}