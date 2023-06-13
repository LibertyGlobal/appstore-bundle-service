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
package com.lgi.appstorebundle.resources;

import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleContext;
import com.lgi.appstorebundle.error.exception.ApplicationNotFoundException;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.service.ApplicationMetadataService;
import com.lgi.appstorebundle.service.BundleService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.lgi.appstorebundle.api.model.BundleStatus.BUNDLE_ERROR;
import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_REQUESTED;
import static com.lgi.appstorebundle.common.Headers.CORRELATION_ID;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;
import static org.springframework.http.HttpStatus.ACCEPTED;

@RestController
@RequestMapping("/applications")
public class AppStoreBundleController {

    private static final Logger LOG = LoggerFactory.getLogger(AppStoreBundleController.class);

    private static final String RETRY_AFTER = "Retry-After";

    private final Duration retryAfter;
    private final ApplicationMetadataService applicationMetadataService;
    private final BundleService bundleService;
    private final boolean encrypt;

    private static final Predicate<Bundle> IS_NOT_BUNDLE_ERROR = application -> BUNDLE_ERROR != application.getStatus();

    @Autowired
    public AppStoreBundleController(@Value("${http.retry.after}") Duration retryAfter,
                                    ApplicationMetadataService applicationMetadataService,
                                    BundleService bundleService,
                                    @Value("${bundle.encryption.enabled}") boolean encrypt) {
        this.retryAfter = checkNotNull(retryAfter, "retryAfter");
        this.applicationMetadataService = applicationMetadataService;
        this.bundleService = bundleService;
        this.encrypt = encrypt;
    }

    @GetMapping(value = "/{appId}/{appVersion}/{platformName}/{firmwareVersion}/{appBundleName}", produces = {"application/json"})
    public ResponseEntity<Object> startBundleGeneration(@Valid @PathVariable("appId") String appId,
                                                        @Valid @PathVariable("appVersion") String appVersion,
                                                        @Valid @PathVariable("platformName") String platformName,
                                                        @Valid @PathVariable("firmwareVersion") String firmwareVersion,
                                                        @Valid @PathVariable("appBundleName") String appBundleName,
                                                        @RequestHeader(CORRELATION_ID) String xRequestId) {
        final ApplicationParams applicationParams = ApplicationParams.create(appId, appVersion, platformName, firmwareVersion, appBundleName);
        final ApplicationMetadata applicationMetadataWithMaintainer = applicationMetadataService.getApplicationMetadata(applicationParams)
                .orElseThrow(() -> ApplicationNotFoundException.createDefault(appId, appVersion, platformName, firmwareVersion));

        final String maintainerCode = applicationMetadataWithMaintainer.getMaintainer().getCode();
        final ApplicationMetadataForMaintainer applicationMetadataForMaintainer = applicationMetadataService.getApplicationMetadataForMaintainerCode(applicationParams, maintainerCode)
                .orElseThrow(() -> ApplicationNotFoundException.createDefault(appId, appVersion, platformName, firmwareVersion));

        final Optional<Bundle> maybeBundle = bundleService.getLatestBundle(appId, appVersion, platformName, firmwareVersion);

        if (maybeBundle.filter(IS_NOT_BUNDLE_ERROR).isEmpty()) {
            final UUID id = randomUUID();
            LOG.info("Starting a new bundle generation for bundle id:'{}', appId:'{}', appVersion:'{}', platformName:'{}', firmwareVersion:'{}'.", id, appId, appVersion, platformName, firmwareVersion);
            final BundleContext bundleContext = createBundleContext(id, appId, appVersion, platformName, firmwareVersion, xRequestId, applicationMetadataForMaintainer.getHeader().getOciImageUrl());
            bundleService.triggerBundleGeneration(bundleContext);
        }

        return ResponseEntity.status(ACCEPTED)
                .header(RETRY_AFTER, String.valueOf(retryAfter.toSeconds()))
                .header(CORRELATION_ID, xRequestId)
                .build();
    }

    private BundleContext createBundleContext(UUID id, String appId, String appVersion, String platformName, String firmwareVersion, String xRequestId, String ociImageUrl) {
        final DateTime messageTimestamp = now(DateTimeZone.UTC);
        final ApplicationContext applicationContext = ApplicationContext.create(appId, appVersion, platformName, firmwareVersion);
        final Bundle bundle = Bundle.create(id, applicationContext, GENERATION_REQUESTED, xRequestId, messageTimestamp);
        return BundleContext.create(bundle, ociImageUrl, encrypt);
    }
}
