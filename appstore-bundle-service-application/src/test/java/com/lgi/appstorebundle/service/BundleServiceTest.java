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

import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleContext;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.exception.RabbitMQException;
import com.lgi.appstorebundle.external.OptionalException;
import com.lgi.appstorebundle.external.RabbitMQService;
import com.lgi.appstorebundle.model.EncryptionMessageFactory;
import com.lgi.appstorebundle.storage.persistent.BundleDao;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.lgi.appstorebundle.api.model.BundleStatus.GENERATION_REQUESTED;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BundleServiceTest {

    public static final UUID ID = randomUUID();
    public static final String APP_ID = "appId";
    public static final String APP_VERSION = "appVersion";
    public static final String PLATFORM_NAME = "platformName";
    public static final String FIRMWARE_VERSION = "firmwareVersion";
    public static final String OCI_IMAGE_URL = "ociImageUrl";
    public static final String X_REQUEST_ID = "xRequestId";

    private final BundleDao dao = mock(BundleDao.class);
    private final RabbitMQService rabbitMQ = mock(RabbitMQService.class);
    private final EncryptionMessageFactory encryptionMessageFactory = mock(EncryptionMessageFactory.class);
    private final BundleService service = new BundleService(dao, rabbitMQ, encryptionMessageFactory);
    private final DateTime messageTimestamp = now(DateTimeZone.UTC);
    private final ApplicationContext applicationContext = ApplicationContext.create(APP_ID, APP_VERSION, PLATFORM_NAME, FIRMWARE_VERSION);
    private final Bundle bundle = Bundle.create(ID, applicationContext, GENERATION_REQUESTED, X_REQUEST_ID, messageTimestamp, true);
    private final BundleContext bundleContext = BundleContext.create(bundle, OCI_IMAGE_URL, true);

    @Captor
    private ArgumentCaptor<BundleStatus> bundleStatusCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        reset(dao, rabbitMQ);
    }

    @Test
    void triggerBundleGeneration_SavingToDBAndSendingMessageExecuted() {
        // GIVEN
        when(rabbitMQ.sendGenerationMessage(any())).thenReturn(OptionalException.empty());

        // WHEN
        service.triggerBundleGeneration(bundleContext);

        // THEN
        verify(dao).saveBundleWithStatus(any());
        verify(rabbitMQ).sendGenerationMessage(any());
        verify(dao, never()).updateStatusForBundle(any(), any(), any());
    }

    @Test
    void triggerBundleGenerationWithException_SaveAndUpdateRowAndSendingMessageExecuted() {
        // GIVEN
        when(rabbitMQ.sendGenerationMessage(any())).thenReturn(OptionalException.of(RabbitMQException.createDefault(X_REQUEST_ID, "error")));

        // WHEN, THEN
        assertThrows(RabbitMQException.class,
                () -> service.triggerBundleGeneration(bundleContext));

        verify(dao).saveBundleWithStatus(any());
        verify(rabbitMQ).sendGenerationMessage(any());
        verify(dao).updateStatusForBundle(any(), any(), any());
    }

    @Test
    void triggerBundleEncryptionWithNotExistingBundle_skipTriggeringEncryption() {
        // GIVEN
        when(dao.getBundle(bundle.getId())).thenReturn(Optional.empty());

        // WHEN, THEN
        service.triggerBundleEncryption(bundle.getId(), X_REQUEST_ID);

        verify(dao, never()).updateStatusForBundle(any(), any(), any());
        verify(rabbitMQ, never()).sendEncryptionMessage(any(), any());
    }

    @Test
    void triggerBundleEncryptionWithExistingBundle_sendsMqMessage() {
        // GIVEN
        when(dao.getBundle(bundle.getId())).thenReturn(Optional.of(bundle));
        when(rabbitMQ.sendEncryptionMessage(any(), any())).thenReturn(OptionalException.empty());

        // WHEN, THEN
        service.triggerBundleEncryption(bundle.getId(), X_REQUEST_ID);

        verify(dao, atMostOnce()).updateStatusForBundle(any(), bundleStatusCaptor.capture(), any());
        verify(rabbitMQ).sendEncryptionMessage(any(), any());
        assertEquals(BundleStatus.ENCRYPTION_REQUESTED, bundleStatusCaptor.getValue());
    }

    @Test
    void triggerBundleEncryptionWithException_updatesBundleStatusToError() {
        // GIVEN
        UUID bundleId = bundle.getId();
        when(dao.getBundle(bundleId)).thenReturn(Optional.of(bundle));
        when(rabbitMQ.sendEncryptionMessage(any(), any())).thenReturn(OptionalException.of(RabbitMQException.createDefault(X_REQUEST_ID, "error")));

        // WHEN, THEN
        assertThrows(RabbitMQException.class,
                () -> service.triggerBundleEncryption(bundleId, X_REQUEST_ID));

        verify(dao, times(2)).updateStatusForBundle(any(), bundleStatusCaptor.capture(), any());
        verify(rabbitMQ).sendEncryptionMessage(any(), any());
        assertEquals(BundleStatus.BUNDLE_ERROR, bundleStatusCaptor.getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testEncryption(Boolean dbValue, boolean expectedValue) {
        // GIVEN
        UUID bundleId = bundle.getId();
        when(dao.isEncryptionEnabled(bundleId)).thenReturn(Optional.ofNullable(dbValue));

        // WHEN
        boolean result = service.isEncryptionEnabled(bundle.getId());

        // THEN
        assertEquals(expectedValue, result);
    }

    private static List<Arguments> testEncryption() {
        return List.of(
                Arguments.of(Boolean.TRUE, true),
                Arguments.of(Boolean.FALSE, false),
                Arguments.of(null, false)
        );
    }
}