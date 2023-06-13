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
package com.lgi.appstorebundle.storage.persistent;

import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.jooq.generated.tables.records.BundleRecord;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.lgi.appstorebundle.jooq.generated.Tables.BUNDLE;
import static java.util.Objects.requireNonNull;
import static org.joda.time.DateTime.now;
import static org.jooq.impl.DSL.coalesce;

@Component
public class JooqBundleDao implements BundleDao {

    private final DSLContext readDslContext;

    private final DSLContext writeDslContext;

    @Autowired
    public JooqBundleDao(DSLContext readDslContext, DSLContext writeDslContext) {
        this.readDslContext = requireNonNull(readDslContext, "readDslContext");
        this.writeDslContext = requireNonNull(writeDslContext, "writeDslContext");
    }

    @Override
    public Optional<Bundle> getLatestBundle(String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        return readDslContext.selectFrom(BUNDLE)
                .where(BUNDLE.APPLICATION_ID.eq(applicationId))
                .and(BUNDLE.APPLICATION_VERSION.eq(applicationVersion))
                .and(BUNDLE.PLATFORM_NAME.eq(platformName))
                .and(BUNDLE.FIRMWARE_VERSION.eq(firmwareVersion))
                .orderBy(coalesce(BUNDLE.UPDATED_AT, BUNDLE.CREATED_AT))
                .limit(1)
                .fetchOptional(JooqBundleDao::toBundle);
    }

    @Override
    public Optional<Bundle> getBundle(UUID id) {
        return readDslContext.selectFrom(BUNDLE)
                .where(BUNDLE.ID.eq(id))
                .fetchOptional(JooqBundleDao::toBundle);
    }

    @Override
    public void saveBundleWithStatus(Bundle bundle) {
        writeDslContext.transaction(configuration ->
                DSL.using(configuration).insertInto(BUNDLE,
                                BUNDLE.ID,
                                BUNDLE.APPLICATION_ID,
                                BUNDLE.APPLICATION_VERSION,
                                BUNDLE.PLATFORM_NAME,
                                BUNDLE.FIRMWARE_VERSION,
                                BUNDLE.STATUS,
                                BUNDLE.X_REQUEST_ID,
                                BUNDLE.CREATED_AT,
                                BUNDLE.MESSAGE_TIMESTAMP
                        ).values(
                                bundle.getId(),
                                bundle.getApplicationContext().getApplicationId(),
                                bundle.getApplicationContext().getApplicationVersion(),
                                bundle.getApplicationContext().getPlatformName(),
                                bundle.getApplicationContext().getFirmwareVersion(),
                                bundle.getStatus().name(),
                                bundle.getXRequestId(),
                                now(),
                                bundle.getMessageTimestamp()
                        )
                        .execute());
    }

    @Override
    public void updateStatusForBundle(UUID id, BundleStatus status, DateTime messageTimestamp) {
        writeDslContext.transaction(configuration ->
                DSL.using(configuration)
                        .update(BUNDLE)
                        .set(BUNDLE.STATUS, status.toString())
                        .set(BUNDLE.UPDATED_AT, now())
                        .set(BUNDLE.MESSAGE_TIMESTAMP, messageTimestamp)
                        .where(BUNDLE.ID.eq(id))
                        .execute());
    }

    @Override
    public boolean updateBundleStatusIfNewer(UUID id, BundleStatus status, DateTime messageTimestamp) {
        final Integer numberOfUpdatedRow = writeDslContext.transactionResult(configuration ->
                DSL.using(configuration)
                        .update(BUNDLE)
                        .set(BUNDLE.STATUS, status.toString())
                        .set(BUNDLE.UPDATED_AT, now())
                        .set(BUNDLE.MESSAGE_TIMESTAMP, messageTimestamp)
                        .where(BUNDLE.ID.eq(id))
                        .and(BUNDLE.MESSAGE_TIMESTAMP.lessThan(messageTimestamp))
                        .execute());
        return numberOfUpdatedRow > 0;
    }

    private static Bundle toBundle(BundleRecord bundleRecord) {
        return Bundle.create(
                bundleRecord.getId(),
                ApplicationContext.create(
                        bundleRecord.get(BUNDLE.APPLICATION_ID),
                        bundleRecord.get(BUNDLE.APPLICATION_VERSION),
                        bundleRecord.get(BUNDLE.PLATFORM_NAME),
                        bundleRecord.get(BUNDLE.FIRMWARE_VERSION)),
                BundleStatus.valueOf(bundleRecord.get(BUNDLE.STATUS)),
                bundleRecord.get(BUNDLE.X_REQUEST_ID),
                bundleRecord.get(BUNDLE.MESSAGE_TIMESTAMP)
        );
    }
}