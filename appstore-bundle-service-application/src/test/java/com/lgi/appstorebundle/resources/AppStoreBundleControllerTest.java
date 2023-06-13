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

import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.error.exception.ApplicationNotFoundException;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Header;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Maintainer;
import com.lgi.appstorebundle.service.ApplicationMetadataService;
import com.lgi.appstorebundle.service.BundleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppStoreBundleControllerTest {

    private static final String RETRY_AFTER = "Retry-After";
    private static final Duration RETRY_AFTER_IN_SECONDS = Duration.ofSeconds(30);
    public static final String APP_ID = "appId";
    public static final String APP_NAME = "appName";
    public static final String APP_VER = "applicationVersion";
    public static final String URL = "url";
    public static final String OCI_IMAGE_URL = "ociImageUrl";
    public static final String PLATFORM_NAME = "platformName";
    public static final String FIRMWARE_VERSION = "firmwareVersion";
    public static final String BUNDLE_NAME = "bundleName";
    public static final String X_REQUEST_ID = "xRequestId";

    private final ApplicationMetadataService asmsService = mock(ApplicationMetadataService.class);
    private final BundleService bundleService = mock(BundleService.class);
    private final AppStoreBundleController resource = new AppStoreBundleController(RETRY_AFTER_IN_SECONDS, asmsService, bundleService, true);
    private final HeaderForMaintainer applicationHeaderForMaintainer = HeaderForMaintainer.create(APP_ID, APP_NAME, APP_VER, URL, OCI_IMAGE_URL);
    private final Header applicationHeader = Header.create(APP_ID, APP_NAME, APP_VER, URL);
    private final Maintainer applicationMaintainer = Maintainer.create("maintainerCode");
    private final ApplicationMetadataForMaintainer applicationMetadataForMaintainer = ApplicationMetadataForMaintainer.create(applicationHeaderForMaintainer);
    private final ApplicationMetadata applicationMetadata = ApplicationMetadata.create(applicationHeader, applicationMaintainer);

    @BeforeEach
    void setUp() {
        reset(asmsService, bundleService);
    }

    @Test
    void returnErrorWhenApplicationNotExistsInAppstoreMetadataService() {
        // GIVEN
        when(asmsService.getApplicationMetadata(any())).thenReturn(Optional.empty());

        // WHEN, THEN
        assertThrows(ApplicationNotFoundException.class, () -> resource.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID));
    }

    @Test
    void returnErrorWhenApplicationForMaintainerNotExistsInAppstoreMetadataService() {
        // GIVEN
        when(asmsService.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(asmsService.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.empty());

        // WHEN, THEN
        assertThrows(ApplicationNotFoundException.class, () -> resource.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID));
    }

    @Test
    void returnRetryAfterAndTriggerBundleGeneration() {
        // GIVEN
        when(asmsService.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(asmsService.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(bundleService.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.empty());

        // WHEN
        ResponseEntity<Object> response = resource.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(bundleService).triggerBundleGeneration(any());
        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(
                String.valueOf(RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }

    @Test
    void returnRetryAfterAndTriggerNewBundleGenerationBecauseOfBundleErrorStatus() {
        // GIVEN
        when(asmsService.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(asmsService.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.BUNDLE_ERROR);
        when(bundleService.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.of(bundle));

        // WHEN
        ResponseEntity<Object> response = resource.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(bundleService).triggerBundleGeneration(any());
        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(String.valueOf(
                RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }

    @Test
    void returnRetryAfterWithoutStartingBundleGeneration() {
        // GIVEN
        when(asmsService.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(asmsService.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.GENERATION_REQUESTED);
        when(bundleService.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.of(bundle));

        // WHEN
        ResponseEntity<Object> response = resource.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(bundleService, never()).triggerBundleGeneration(any());
        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(
                String.valueOf(RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }
}