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
package com.lgi.appstorebundle.util;

import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.lgi.appstorebundle.service.BundleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptionHelperTest {

    private final BundleService bundleService = mock(BundleService.class);

    @Test
    void testEncryptionEnabledForApplicationMetadata() {
        //GIVEN
        EncryptionHelper encryptionHelper = new EncryptionHelper(true, bundleService);
        ApplicationMetadataForMaintainer applicationMetadataForMaintainer = createMockApplicationMetadataForMaintainer(true);

        //WHEN
        boolean isEncryptionEnabled = encryptionHelper.isEncryptionEnabled(applicationMetadataForMaintainer);

        //THEN
        assertTrue(isEncryptionEnabled);
    }

    @Test
    void testEncryptionEnabledForFeedbackMessage() {
        //GIVEN
        EncryptionHelper encryptionHelper = new EncryptionHelper(true, bundleService);
        FeedbackMessage feedbackMessage = mock(FeedbackMessage.class);
        when(bundleService.isEncryptionEnabled(any())).thenReturn(true);

        //WHEN
        boolean isEncryptionEnabled = encryptionHelper.isEncryptionEnabled(feedbackMessage);

        //THEN
        assertTrue(isEncryptionEnabled);
    }

    @ParameterizedTest
    @MethodSource("testEncryptionDisabled")
    void testEncryptionDisabledForApplicationMetadata(boolean encryptionForHelper, boolean encryptionForMaintainer) {
        //GIVEN
        EncryptionHelper encryptionHelper = new EncryptionHelper(encryptionForHelper, bundleService);
        ApplicationMetadataForMaintainer applicationMetadataForMaintainer = createMockApplicationMetadataForMaintainer(encryptionForMaintainer);

        //WHEN
        boolean isEncryptionEnabled = encryptionHelper.isEncryptionEnabled(applicationMetadataForMaintainer);

        //THEN
        assertFalse(isEncryptionEnabled);
    }

    @ParameterizedTest
    @MethodSource("testEncryptionDisabled")
    void testEncryptionDisabledForFeedbackMessage(boolean encryptionForHelper, boolean encryptionForBundle) {
        //GIVEN
        EncryptionHelper encryptionHelper = new EncryptionHelper(encryptionForHelper, bundleService);
        FeedbackMessage feedbackMessage = mock(FeedbackMessage.class);
        when(bundleService.isEncryptionEnabled(any())).thenReturn(encryptionForBundle);

        //WHEN
        boolean isEncryptionEnabled = encryptionHelper.isEncryptionEnabled(feedbackMessage);

        //THEN
        assertFalse(isEncryptionEnabled);
    }

    private static List<Arguments> testEncryptionDisabled() {
        return List.of(
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false)
        );
    }

    private ApplicationMetadataForMaintainer createMockApplicationMetadataForMaintainer(boolean isEncryptionEnabled) {
        HeaderForMaintainer headerForMaintainer = mock(HeaderForMaintainer.class);
        when(headerForMaintainer.getEncryption()).thenReturn(isEncryptionEnabled);

        ApplicationMetadataForMaintainer applicationMetadataForMaintainer = mock(ApplicationMetadataForMaintainer.class);
        when(applicationMetadataForMaintainer.getHeader()).thenReturn(headerForMaintainer);

        return applicationMetadataForMaintainer;
    }
}
