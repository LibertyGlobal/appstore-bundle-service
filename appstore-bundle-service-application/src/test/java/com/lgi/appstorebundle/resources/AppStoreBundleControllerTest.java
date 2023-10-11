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
import com.lgi.appstorebundle.api.model.BundleContext;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.error.exception.ApplicationNotFoundException;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Header;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Maintainer;
import com.lgi.appstorebundle.service.ApplicationMetadataService;
import com.lgi.appstorebundle.service.BundleService;
import com.lgi.appstorebundle.util.EncryptionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    public static final boolean ENCRYPTION_ENABLED = true;
    public static final boolean ENCRYPTION_DISABLED = false;
    public static final String OCI_IMAGE_URL = "ociImageUrl";
    public static final String PLATFORM_NAME = "platformName";
    public static final String FIRMWARE_VERSION = "firmwareVersion";
    public static final String BUNDLE_NAME = "bundleName";
    public static final String X_REQUEST_ID = "xRequestId";

    private static final ApplicationMetadataService ASMS_SERVICE = mock(ApplicationMetadataService.class);
    private static final BundleService BUNDLE_SERVICE = mock(BundleService.class);
    private static final AppStoreBundleController RESOURCE_ENCRYPTION_ENABLED = new AppStoreBundleController(RETRY_AFTER_IN_SECONDS, ASMS_SERVICE, BUNDLE_SERVICE, new EncryptionHelper(ENCRYPTION_ENABLED, BUNDLE_SERVICE));
    private static final AppStoreBundleController RESOURCE_ENCRYPTION_DISABLED = new AppStoreBundleController(RETRY_AFTER_IN_SECONDS, ASMS_SERVICE, BUNDLE_SERVICE, new EncryptionHelper(ENCRYPTION_DISABLED, BUNDLE_SERVICE));
    private static final HeaderForMaintainer APPLICATION_HEADER_FOR_MAINTAINER_ENCRYPTION_ENABLED = HeaderForMaintainer.create(APP_ID, APP_NAME, APP_VER, URL, ENCRYPTION_ENABLED, OCI_IMAGE_URL);
    private static final HeaderForMaintainer APPLICATION_HEADER_FOR_MAINTAINER_ENCRYPTION_DISABLED = HeaderForMaintainer.create(APP_ID, APP_NAME, APP_VER, URL, ENCRYPTION_DISABLED, OCI_IMAGE_URL);
    private static final Header applicationHeader = Header.create(APP_ID, APP_NAME, APP_VER, URL);
    private static final Maintainer applicationMaintainer = Maintainer.create("maintainerCode");
    private static final ApplicationMetadataForMaintainer APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_DISABLED = ApplicationMetadataForMaintainer.create(APPLICATION_HEADER_FOR_MAINTAINER_ENCRYPTION_DISABLED);
    private static final ApplicationMetadataForMaintainer APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED = ApplicationMetadataForMaintainer.create(APPLICATION_HEADER_FOR_MAINTAINER_ENCRYPTION_ENABLED);
    private static final ApplicationMetadata applicationMetadata = ApplicationMetadata.create(applicationHeader, applicationMaintainer);

    @BeforeEach
    void setUp() {
        reset(ASMS_SERVICE, BUNDLE_SERVICE);
    }

    @Test
    void returnErrorWhenApplicationNotExistsInAppstoreMetadataService() {
        // GIVEN
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.empty());

        // WHEN, THEN
        assertThrows(ApplicationNotFoundException.class, () -> RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID));
    }

    @Test
    void returnErrorWhenApplicationForMaintainerNotExistsInAppstoreMetadataService() {
        // GIVEN
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.empty());

        // WHEN, THEN
        assertThrows(ApplicationNotFoundException.class, () -> RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID));
    }

    @Test
    void returnRetryAfterAndTriggerBundleGeneration() {
        // GIVEN
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED));
        when(BUNDLE_SERVICE.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.empty());

        // WHEN
        ResponseEntity<Object> response = RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(BUNDLE_SERVICE).triggerBundleGeneration(any());
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
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED));
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.BUNDLE_ERROR);
        when(BUNDLE_SERVICE.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.of(bundle));

        // WHEN
        ResponseEntity<Object> response = RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(BUNDLE_SERVICE).triggerBundleGeneration(any());
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
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED));
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.GENERATION_REQUESTED);
        when(BUNDLE_SERVICE.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.of(bundle));

        // WHEN
        ResponseEntity<Object> response = RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        verify(BUNDLE_SERVICE, never()).triggerBundleGeneration(any());
        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(
                String.valueOf(RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }

    @Test
    void returnRetryAfterWithEncryptionEnabled() {
        // GIVEN
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED));
        when(BUNDLE_SERVICE.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.empty());

        // WHEN
        ResponseEntity<Object> response = RESOURCE_ENCRYPTION_ENABLED.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        ArgumentCaptor<BundleContext> argumentCaptor = ArgumentCaptor.forClass(BundleContext.class);
        verify(BUNDLE_SERVICE).triggerBundleGeneration(argumentCaptor.capture());
        BundleContext capturedArgument = argumentCaptor.getValue();
        assertTrue(capturedArgument.getEncrypt());

        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(
                String.valueOf(RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }

    @ParameterizedTest
    @MethodSource("getCasesWhenEncryptedDisabled")
    void returnRetryAfterWithEncryptionDisabled(ApplicationMetadataForMaintainer applicationMetadataForMaintainer,
                                                AppStoreBundleController appStoreBundleController) {
        // GIVEN
        when(ASMS_SERVICE.getApplicationMetadata(any())).thenReturn(Optional.of(applicationMetadata));
        when(ASMS_SERVICE.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(BUNDLE_SERVICE.getLatestBundle(any(), any(), any(), any())).thenReturn(Optional.empty());

        // WHEN
        ResponseEntity<Object> response = appStoreBundleController.startBundleGeneration(
                APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VERSION, BUNDLE_NAME, X_REQUEST_ID);

        // THEN
        ArgumentCaptor<BundleContext> argumentCaptor = ArgumentCaptor.forClass(BundleContext.class);
        verify(BUNDLE_SERVICE).triggerBundleGeneration(argumentCaptor.capture());
        BundleContext capturedArgument = argumentCaptor.getValue();
        assertFalse(capturedArgument.getEncrypt());

        assertEquals(SC_ACCEPTED, response.getStatusCodeValue(), "Service should return ACCEPTED status code");
        assertNotNull(response.getHeaders().get(RETRY_AFTER), "Service should return ACCEPTED status code");
        assertEquals(
                String.valueOf(RETRY_AFTER_IN_SECONDS.toSeconds()),
                response.getHeaders().get(RETRY_AFTER).get(0), "Service should return header 'Retry-After'"
        );
    }

    private static List<Arguments> getCasesWhenEncryptedDisabled() {
        return List.of(
                Arguments.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_DISABLED, RESOURCE_ENCRYPTION_ENABLED),
                Arguments.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_ENABLED, RESOURCE_ENCRYPTION_DISABLED),
                Arguments.of(APPLICATION_METADATA_FOR_MAINTAINER_ENCRYPTION_DISABLED, RESOURCE_ENCRYPTION_DISABLED)
        );
    }
}