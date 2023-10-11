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

import com.lgi.appstorebundle.api.model.Bundle;
import com.lgi.appstorebundle.api.model.BundleStatus;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.UUID;

public interface BundleDao {

    Optional<Bundle> getLatestBundle(String applicationId, String applicationVersion, String platformName, String firmwareVersion);

    Optional<Bundle> getBundle(UUID id);

    void saveBundleWithStatus(Bundle bundle);

    void updateStatusForBundle(UUID id, BundleStatus status, DateTime messageTimestamp);

    boolean updateBundleStatusIfNewer(UUID id, BundleStatus status, DateTime messageTimestamp);

    Optional<Boolean> isEncryptionEnabled(UUID id);

}
