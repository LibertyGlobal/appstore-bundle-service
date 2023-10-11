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
package com.lgi.appstorebundle.service;

import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleContext;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.external.OptionalException;
import com.lgi.appstorebundle.external.RabbitMQService;
import com.lgi.appstorebundle.model.EncryptionMessageFactory;
import com.lgi.appstorebundle.storage.persistent.BundleDao;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.lgi.appstorebundle.api.model.BundleStatus.BUNDLE_ERROR;

@Service
public class BundleService {

    private static final Logger LOG = LoggerFactory.getLogger(BundleService.class);

    private final BundleDao bundleDao;
    private final RabbitMQService rabbitMqService;
    private final EncryptionMessageFactory encryptionMessageFactory;

    @Autowired
    public BundleService(BundleDao bundleDao, RabbitMQService rabbitMqService, EncryptionMessageFactory encryptionMessageFactory) {
        this.bundleDao = bundleDao;
        this.rabbitMqService = rabbitMqService;
        this.encryptionMessageFactory = encryptionMessageFactory;
    }

    public Optional<Bundle> getLatestBundle(String applicationId, String applicationVersion, String platformName, String firmwareVersion) {
        return bundleDao.getLatestBundle(applicationId, applicationVersion, platformName, firmwareVersion);
    }

    public void triggerBundleGeneration(BundleContext bundleContext) {
        final Bundle bundle = bundleContext.getBundle();
        bundleDao.saveBundleWithStatus(bundle);
        final var maybeException = rabbitMqService.sendGenerationMessage(bundleContext);
        maybeException.ifPresent(exception -> {
            bundleDao.updateStatusForBundle(bundle.getId(), BUNDLE_ERROR, bundle.getMessageTimestamp());
            throw exception;
        });
    }

    public void updateBundleStatusIfNewer(UUID id, BundleStatus status, DateTime messageTimestamp) {
        final boolean wasUpdated = bundleDao.updateBundleStatusIfNewer(id, status, messageTimestamp);
        if (wasUpdated) {
            LOG.info("Bundle for id: '{}' was updated", id);
        }
    }

    public void triggerBundleEncryption(UUID id, String xRequestId) {
        LOG.info("Triggering bundle encryption for bundle id '{}", id);
        final Optional<Bundle> maybeBundle = bundleDao.getBundle(id);
        maybeBundle.ifPresentOrElse(bundle -> {
            bundleDao.updateStatusForBundle(bundle.getId(), BundleStatus.ENCRYPTION_REQUESTED, DateTime.now(DateTimeZone.UTC));
            final OptionalException optionalException =
                    rabbitMqService.sendEncryptionMessage(encryptionMessageFactory.fromBundle(bundle), xRequestId);
            optionalException.ifPresent(exception -> {
                bundleDao.updateStatusForBundle(bundle.getId(), BundleStatus.BUNDLE_ERROR, DateTime.now(DateTimeZone.UTC));
                throw exception;
            });
        }, () -> LOG.warn("Could not find a bundle for id '{}', will not send it for encryption.", id));
    }

    public boolean isEncryptionEnabled(UUID id) {
        return bundleDao.isEncryptionEnabled(id).orElseGet(() -> {
            LOG.warn("Could not find a bundle for id '{}', will not send it for encryption.", id);
            return false;
        });
    }
}
