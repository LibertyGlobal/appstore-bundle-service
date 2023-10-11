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
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.lgi.appstorebundle.service.BundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class EncryptionHelper {
    private boolean encrypt;

    private BundleService bundleService;

    @Autowired
    public EncryptionHelper(@Value("${bundle.encryption.enabled}") boolean encrypt, BundleService bundleService) {
        this.encrypt = encrypt;
        this.bundleService = checkNotNull(bundleService, "bundleService");
    }


    public boolean isEncryptionEnabled(ApplicationMetadataForMaintainer applicationMetadataForMaintainer) {
        return encrypt && Boolean.TRUE.equals(applicationMetadataForMaintainer.getHeader().getEncryption());
    }

    public boolean isEncryptionEnabled(FeedbackMessage feedbackMessage) {
        return encrypt && bundleService.isEncryptionEnabled(feedbackMessage.getId());
    }
}
