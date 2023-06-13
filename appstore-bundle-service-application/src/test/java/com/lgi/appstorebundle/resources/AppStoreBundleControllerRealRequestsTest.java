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
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.exception.RabbitMQException;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Header;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Maintainer;
import com.lgi.appstorebundle.service.ApplicationMetadataService;
import com.lgi.appstorebundle.service.BundleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppStoreBundleController.class)
class AppStoreBundleControllerRealRequestsTest {

    private static final String GET_APPLICATION_PATH = "/applications/{appId}/{appVersion}/{platformName}/{firmwareVersion}/{appBundleName}";
    private static final String CORRELATION_ID = "x-request-id";
    private static final String APP_ID = "applicationId";
    private static final String APP_VER = "applicationVersion";
    private static final String APP_NAME = "applicationName";
    private static final String URL = "url";
    private static final String PLATFORM_NAME = "platformName";
    private static final String FIRMWARE_VER = "firmwareVersion";
    private static final String OCI_IMAGE_URL = "ociImageUrl";
    private static final String APP_BUNDLE_NAME = "applicationBundleName";
    private static final String X_REQUEST_ID = "0293e324-0558-4f23-b924-25a8b3f59583";
    private final HeaderForMaintainer applicationHeaderForMaintainer = HeaderForMaintainer.create(APP_ID, APP_NAME, APP_VER, URL, OCI_IMAGE_URL);
    private final Header applicationHeader = Header.create(APP_ID, APP_NAME, APP_VER, URL);
    private final Maintainer applicationMaintainer = Maintainer.create("maintainerCode");
    private final ApplicationMetadataForMaintainer applicationMetadataForMaintainer = ApplicationMetadataForMaintainer.create(applicationHeaderForMaintainer);
    private final ApplicationMetadata applicationMetadata = ApplicationMetadata.create(applicationHeader, applicationMaintainer);

    @Value("${http.retry.after}")
    private Duration retryAfter;

    @Autowired
    private AppStoreBundleController bundleController;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationMetadataService applicationMetadataServiceMock;

    @MockBean
    private BundleService bundleServiceMock;

    @Test
    void givenValidRequestWhenApplicationDoesNotExistThenNotFound() throws Exception {
        // GIVEN
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.empty());

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpectAll(
                status().is(HttpStatus.NOT_FOUND.value()),
                header().doesNotExist("Retry-After")
        );
    }

    @Test
    void givenValidRequestWhenApplicationForMaintainerDoesNotExistThenNotFound() throws Exception {
        // GIVEN
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.of(applicationMetadata));
        when(applicationMetadataServiceMock.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.empty());

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpectAll(
                status().is(HttpStatus.NOT_FOUND.value()),
                header().doesNotExist("Retry-After")
        );
    }

    @Test
    void givenValidRequestWhenGenerationNotStartedThenGenerationRequested() throws Exception {
        // GIVEN
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.of(applicationMetadata));
        when(applicationMetadataServiceMock.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(bundleServiceMock.getLatestBundle(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER)).thenReturn(Optional.empty());

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpectAll(
                status().is(HttpStatus.ACCEPTED.value()),
                header().string("Retry-After", String.valueOf(retryAfter.toSeconds()))
        );
    }

    @Test
    void givenValidRequestWhenGenerationErrorThenTriggerNewGeneration() throws Exception {
        // GIVEN
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.BUNDLE_ERROR);
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.of(applicationMetadata));
        when(applicationMetadataServiceMock.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(bundleServiceMock.getLatestBundle(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER)).thenReturn(Optional.of(bundle));

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpectAll(
                status().is(HttpStatus.ACCEPTED.value()),
                header().string("Retry-After", String.valueOf(retryAfter.toSeconds()))
        );
    }

    @Test
    void givenValidRequestWhenGenerationRequestedThenGenerationSkipped() throws Exception {
        // GIVEN
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getStatus()).thenReturn(BundleStatus.GENERATION_REQUESTED);
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.of(applicationMetadata));
        when(applicationMetadataServiceMock.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(bundleServiceMock.getLatestBundle(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER)).thenReturn(Optional.of(bundle));

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpectAll(
                status().is(HttpStatus.ACCEPTED.value()),
                header().string("Retry-After", String.valueOf(retryAfter.toSeconds()))
        );
    }

    @Test
    void givenValidRequestWhenTriggeringGenerationThrowsExceptionThenInternalServerError() throws Exception {
        // GIVEN
        when(applicationMetadataServiceMock.getApplicationMetadata(any(ApplicationParams.class))).thenReturn(Optional.of(applicationMetadata));
        when(applicationMetadataServiceMock.getApplicationMetadataForMaintainerCode(any(), any())).thenReturn(Optional.of(applicationMetadataForMaintainer));
        when(bundleServiceMock.getLatestBundle(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER)).thenReturn(Optional.empty());
        doThrow(RabbitMQException.createDefault(X_REQUEST_ID, "error")).when(bundleServiceMock).triggerBundleGeneration(any());

        // WHEN THEN
        mockMvc.perform(get(GET_APPLICATION_PATH, APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME)
                .header(CORRELATION_ID, X_REQUEST_ID)
        ).andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
