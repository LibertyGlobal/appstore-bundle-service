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
package com.lgi.appstorebundle.model;

import com.lgi.appstorebundle.api.Environment;
import com.lgi.appstorebundle.api.model.ApplicationContext;
import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionMessageFactoryTest {

    private static final String X_REQ_ID = "x-request-id-value";
    private static final Environment ENV = Environment.DEV;
    private static final String BUNDLE_EXTENSION = "tar.gz";
    private static final boolean ENCRYPTION_DISABLED = false;

    private static final Bundle BUNDLE = Bundle.create(randomUUID(),
            ApplicationContext.create("appId", "appVer", "platformName", "firmwareVer"),
            BundleStatus.ENCRYPTION_LAUNCHED, X_REQ_ID, DateTime.now(DateTimeZone.UTC), ENCRYPTION_DISABLED);

    private EncryptionMessageFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EncryptionMessageFactory(ENV, BUNDLE_EXTENSION);
    }

    @Test
    void doesNotAllowNullEnvironmentAndBundleExtension() {
        assertThrows(NullPointerException.class, () -> new EncryptionMessageFactory(null, BUNDLE_EXTENSION));
        assertThrows(NullPointerException.class, () -> new EncryptionMessageFactory(ENV, null));
    }

    @Test
    void whenCreateEncryptionMessageFromValidBundleThenCreatedWithValidOciBundleUrl() {
        //WHEN
        final EncryptionMessage encryptionMessage = factory.fromBundle(BUNDLE);

        //THEN
        assertEquals(ENV.toString(), encryptionMessage.getEnvironment());

        final ApplicationContext appCtx = BUNDLE.getApplicationContext();
        assertEquals(appCtx.getApplicationId(), encryptionMessage.getAppId());
        assertEquals(appCtx.getApplicationVersion(), encryptionMessage.getAppVersion());
        assertEquals(appCtx.getPlatformName(), encryptionMessage.getPlatformName());
        assertEquals(appCtx.getFirmwareVersion(), encryptionMessage.getFirmwareVersion());

        assertEquals("/appId/appVer/platformName/firmwareVer/appId-appVer-platformName-firmwareVer.tar.gz", encryptionMessage.getOciBundleUrl());
    }
}