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

import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.test.service.model.BundleWithAudit;
import io.qameta.allure.Step;
import org.assertj.core.api.SoftAssertions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lgi.appstorebundle.jooq.generated.Tables.BUNDLE;
import static java.util.Comparator.comparing;

public class DatabaseSteps {

    private final DSLContext dslContext;

    public DatabaseSteps(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Step("Get Bundle from DB AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}")
    public Optional<BundleWithAudit> getBundleWithAudit(String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        return dslContext.selectFrom(BUNDLE)
                .where(BUNDLE.APPLICATION_ID.eq(applicationId))
                .and(BUNDLE.APPLICATION_VERSION.eq(applicationVersion))
                .and(BUNDLE.PLATFORM_NAME.eq(platformName))
                .and(BUNDLE.FIRMWARE_VERSION.eq(firmwareVersion))
                .fetchOptional(r -> BundleWithAudit.create(
                        Bundle.create(
                                r.getId(),
                                ApplicationContext.create(
                                        r.getApplicationId(),
                                        r.getApplicationVersion(),
                                        r.getPlatformName(),
                                        r.getFirmwareVersion()),
                                BundleStatus.valueOf(r.getStatus()),
                                r.getXRequestId(),
                                r.getMessageTimestamp()
                        ),
                        r.getCreatedAt(),
                        r.getUpdatedAt())
                );
    }

    @Step("Get Bundle from DB Id: {0}")
    public Optional<BundleWithAudit> getBundleWithAudit(UUID id) {
        return dslContext.selectFrom(BUNDLE)
                .where(BUNDLE.ID.eq(id))
                .fetchOptional(r -> BundleWithAudit.create(
                        Bundle.create(
                                r.getId(),
                                ApplicationContext.create(
                                        r.getApplicationId(),
                                        r.getApplicationVersion(),
                                        r.getPlatformName(),
                                        r.getFirmwareVersion()),
                                BundleStatus.valueOf(r.getStatus()),
                                r.getXRequestId(),
                                r.getMessageTimestamp()
                        ),
                        r.getCreatedAt(),
                        r.getUpdatedAt())
                );
    }

    @Step("Get list of Bundle rows from DB AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}")
    public List<BundleWithAudit> getBundlesWithAudit(String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        return dslContext.selectFrom(BUNDLE)
                .where(BUNDLE.APPLICATION_ID.eq(applicationId))
                .and(BUNDLE.APPLICATION_VERSION.eq(applicationVersion))
                .and(BUNDLE.PLATFORM_NAME.eq(platformName))
                .and(BUNDLE.FIRMWARE_VERSION.eq(firmwareVersion))
                .fetch(r -> BundleWithAudit.create(
                        Bundle.create(
                                r.getId(),
                                ApplicationContext.create(
                                        r.getApplicationId(),
                                        r.getApplicationVersion(),
                                        r.getPlatformName(),
                                        r.getFirmwareVersion()),
                                BundleStatus.valueOf(r.getStatus()),
                                r.getXRequestId(),
                                r.getMessageTimestamp()
                        ),
                        r.getCreatedAt(),
                        r.getUpdatedAt())
                );
    }

    @Step("Save Bundle in DB: {0}")
    public void saveBundleWithStatus(BundleWithAudit bundleWithAudit) {
        dslContext.transaction(configuration ->
        {
            final Bundle bundle = bundleWithAudit.getBundle();
            final ApplicationContext applicationContext = bundle.getApplicationContext();
            DSL.using(configuration).insertInto(BUNDLE,
                            BUNDLE.ID,
                            BUNDLE.APPLICATION_ID,
                            BUNDLE.APPLICATION_VERSION,
                            BUNDLE.PLATFORM_NAME,
                            BUNDLE.FIRMWARE_VERSION,
                            BUNDLE.STATUS,
                            BUNDLE.X_REQUEST_ID,
                            BUNDLE.MESSAGE_TIMESTAMP,
                            BUNDLE.CREATED_AT,
                            BUNDLE.UPDATED_AT
                    ).values(
                            bundle.getId(),
                            applicationContext.getApplicationId(),
                            applicationContext.getApplicationVersion(),
                            applicationContext.getPlatformName(),
                            applicationContext.getFirmwareVersion(),
                            bundle.getStatus().name(),
                            bundle.getXRequestId(),
                            bundle.getMessageTimestamp(),
                            bundleWithAudit.getCreatedAt(),
                            bundleWithAudit.getUpdatedAt()
                    )
                    .execute();
        });
    }

    @Step("Clear 'bundle' table")
    public void clearBundleTable() {
        dslContext.deleteFrom(BUNDLE).execute();
    }

    @Step("Verify bundle AppId: {1}, AppVer: {2}, PlatformName: {3}, FirmwareVer: {4} not store in DB")
    public void verifyNotStoredBundle(SoftAssertions softly, String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        final Optional<BundleWithAudit> application = getBundleWithAudit(applicationId, applicationVersion, platformName, firmwareVersion);
        softly.assertThat(application)
                .as("Bundle stored in DB but should not be.")
                .isEmpty();
    }

    @Step("Verify bundle was created in DB, AppId: {1}, AppVer: {2}, PlatformName: {3}, FirmwareVer: {4}, Status: {5}, xRequestId: {6}")
    public void verifyBundleWasCreated(SoftAssertions softly, String applicationId, String applicationVersion, String platformName, String firmwareVersion, BundleStatus status, String xRequestId) {
        final Optional<BundleWithAudit> actualBundleWithAudit = getBundleWithAudit(
                applicationId,
                applicationVersion,
                platformName,
                firmwareVersion);
        softly.assertThat(actualBundleWithAudit)
                .as("Bundle not stored in DB.")
                .isNotEmpty();

        final Bundle actualBundle = actualBundleWithAudit.get().getBundle();

        softly.assertThat(actualBundle.getStatus())
                .as("Bundle from DB contains a wrong Status.")
                .isEqualTo(status);
        softly.assertThat(actualBundle.getXRequestId())
                .as("Bundle from DB contains a wrong xRequestId.")
                .isEqualTo(xRequestId);
        softly.assertThat(actualBundle.getMessageTimestamp())
                .as("Bundle from DB should contain a MessageTimestamp.")
                .isNotNull();
        softly.assertThat(actualBundleWithAudit.get().getCreatedAt())
                .as("Bundle from DB should contain a createdAt.")
                .isNotNull();
        softly.assertThat(actualBundleWithAudit.get().getUpdatedAt())
                .as("Bundle from DB should not contain a updatedAt.")
                .isNull();
    }

    @Step("Verify bundle was updated in DB, Id: {1}, AppId: {2}, AppVer: {3}, PlatformName: {4}, FirmwareVer: {5}, Status: {6}, xRequestId: {7}, messageTimestampBeforeUpdate: {8}, createdAtBeforeUpdate: {9}, updatedAtBeforeUpdate: {10}")
    public void verifyBundleWasUpdated(SoftAssertions softly, UUID id, String expectedAppId, String expectedAppVer, String expectedPlatformName, String expectedFirmwareVersion, BundleStatus expectedStatus, String expectedXRequestId, DateTime messageTimestampForOldRow, DateTime createdAtForOldRow, DateTime updatedAtForOldRow) {
        final Optional<BundleWithAudit> actualBundleWithAudit = getBundleWithAudit(id);
        softly.assertThat(actualBundleWithAudit)
                .as("Bundle not stored in DB.")
                .isNotEmpty();

        final Bundle actualBundle = actualBundleWithAudit.get().getBundle();
        final ApplicationContext applicationContext = actualBundle.getApplicationContext();
        softly.assertThat(applicationContext.getApplicationId())
                .as("Bundle from DB contains a wrong AppId.")
                .isEqualTo(expectedAppId);
        softly.assertThat(applicationContext.getApplicationVersion())
                .as("Bundle from DB contains a wrong AppVersion.")
                .isEqualTo(expectedAppVer);
        softly.assertThat(applicationContext.getPlatformName())
                .as("Bundle from DB contains a wrong PlatformName.")
                .isEqualTo(expectedPlatformName);
        softly.assertThat(applicationContext.getFirmwareVersion())
                .as("Bundle from DB contains a wrong FirmwareVersion.")
                .isEqualTo(expectedFirmwareVersion);
        softly.assertThat(actualBundle.getStatus())
                .as("Bundle from DB contains a wrong Status.")
                .isEqualTo(expectedStatus);
        softly.assertThat(actualBundle.getXRequestId())
                .as("Bundle from DB contains a wrong xRequestId.")
                .isEqualTo(expectedXRequestId);
        softly.assertThat(actualBundle.getMessageTimestamp().withZone(DateTimeZone.UTC))
                .as("Bundle from DB contains a wrong MessageTimestamp.")
                .isGreaterThan(messageTimestampForOldRow);
        softly.assertThat(actualBundleWithAudit.get().getCreatedAt().withZone(DateTimeZone.UTC))
                .as("Bundle from DB contains a wrong createdAt.")
                .isEqualTo(createdAtForOldRow);
        softly.assertThat(actualBundleWithAudit.get().getUpdatedAt().withZone(DateTimeZone.UTC))
                .as("Bundle from DB should contain a updatedAt.")
                .isNotNull();
        Optional.ofNullable(updatedAtForOldRow).ifPresent(dateTime ->
                softly.assertThat(actualBundleWithAudit.get().getUpdatedAt().withZone(DateTimeZone.UTC))
                        .as("Bundle from DB contains a wrong updatedAt.")
                        .isGreaterThan(dateTime)
        );
    }

    @Step("Verify second bundle was added in DB, AppId: {1}, AppVer: {2}, PlatformName: {3}, FirmwareVer: {4}, Status: {5}, xRequestId: {6}, messageTimestampBeforeUpdate: {7}, createdAtBeforeUpdate: {8}, updatedAtBeforeUpdate: {9}")
    public void verifySecondBundleWasAdded(SoftAssertions softly, String applicationId, String applicationVersion, String platformName, String firmwareVersion, BundleStatus expectedStatus, String expectedSecondXRequestId, DateTime messageTimestampForOldRow, DateTime updatedAtForOldRow) {
        final List<BundleWithAudit> actualBundlesWithAudit = getBundlesWithAudit(
                applicationId,
                applicationVersion,
                platformName,
                firmwareVersion);
        softly.assertThat(actualBundlesWithAudit)
                .as("Two bundles should be stored in DB.")
                .hasSize(2);

        final BundleWithAudit actualSecondBundleWithAudit = actualBundlesWithAudit.stream()
                .max(comparing(o -> (o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt())))
                .get();
        final Bundle actualSecondBundle = actualSecondBundleWithAudit
                .getBundle();

        softly.assertThat(actualSecondBundle.getStatus())
                .as("Bundle from DB contains a wrong Status.")
                .isEqualTo(expectedStatus);
        softly.assertThat(actualSecondBundle.getXRequestId())
                .as("Bundle from DB contains a wrong xRequestId.")
                .isEqualTo(expectedSecondXRequestId);
        softly.assertThat(actualSecondBundle.getMessageTimestamp().withZone(DateTimeZone.UTC))
                .as("Bundle from DB contains a wrong MessageTimestamp.")
                .isGreaterThan(messageTimestampForOldRow);
        Optional.ofNullable(updatedAtForOldRow).ifPresent(dateTime ->
                softly.assertThat(actualSecondBundleWithAudit.getCreatedAt().withZone(DateTimeZone.UTC))
                        .as("Bundle from DB should contain a createdAt greater then an updatedAt for old row.")
                        .isGreaterThan(dateTime));
        softly.assertThat(actualSecondBundleWithAudit.getUpdatedAt())
                .as("Bundle from DB should not contain a updatedAt.")
                .isNull();
    }

    @Step("Verify bundle was not updated in DB, Id: {1}, Status: {2}, messageTimestamp: {3}, updatedAt: {4}")
    public void verifyBundleWasNotUpdated(SoftAssertions softly, UUID id, BundleStatus expectedStatus, DateTime expectedMessageTimestamp, DateTime updatedAtForOldRow) {
        final Optional<BundleWithAudit> actualBundleWithAudit = getBundleWithAudit(id);
        softly.assertThat(actualBundleWithAudit)
                .as("Bundle not stored in DB.")
                .isNotEmpty();

        final Bundle actualBundle = actualBundleWithAudit.get().getBundle();
        softly.assertThat(actualBundle.getStatus())
                .as("Bundle from DB contains a wrong Status.")
                .isEqualTo(expectedStatus);
        softly.assertThat(actualBundle.getMessageTimestamp().withZone(DateTimeZone.UTC))
                .as("Bundle from DB contains a wrong MessageTimestamp.")
                .isEqualTo(expectedMessageTimestamp);
        Optional.ofNullable(updatedAtForOldRow).ifPresentOrElse(
                dateTime -> softly.assertThat(actualBundleWithAudit.get().getUpdatedAt().withZone(DateTimeZone.UTC))
                        .as("Bundle from DB contains a wrong updatedAt.")
                        .isEqualTo(dateTime),
                () -> softly.assertThat(actualBundleWithAudit.get().getUpdatedAt())
                        .as("Bundle from DB should not contain a updatedAt.")
                        .isNull()
        );
    }

    @Step("Verify second bundle was not added in DB, AppId: {1}, AppVer: {2}, PlatformName: {3}, FirmwareVer: {4}, Status: {5}, xRequestId: {6}, messageTimestampBeforeDoingUpdate: {7}, createdAtBeforeDoingUpdate: {8}, updatedAtBeforeDoingUpdate: {9}")
    public void verifySecondBundleWasNotAdded(SoftAssertions softly, String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        final List<BundleWithAudit> actualBundleWithAudit = getBundlesWithAudit(
                applicationId,
                applicationVersion,
                platformName,
                firmwareVersion);
        softly.assertThat(actualBundleWithAudit)
                .as("Second bundle not stored in DB.")
                .hasSize(1);
    }
}