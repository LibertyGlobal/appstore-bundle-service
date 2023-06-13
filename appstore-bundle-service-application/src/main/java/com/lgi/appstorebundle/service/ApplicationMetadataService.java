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

import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.external.asms.AppstoreMetadataServiceClient;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApplicationMetadataService {

    private final AppstoreMetadataServiceClient appstoreMetadataServiceClient;

    public ApplicationMetadataService(AppstoreMetadataServiceClient appstoreMetadataServiceClient) {
        this.appstoreMetadataServiceClient = appstoreMetadataServiceClient;
    }

    public Optional<ApplicationMetadata> getApplicationMetadata(ApplicationParams app) {
        return appstoreMetadataServiceClient.getApplicationByAppId(app);
    }

    public Optional<ApplicationMetadataForMaintainer> getApplicationMetadataForMaintainerCode(ApplicationParams app, String maintainerCode) {
        return appstoreMetadataServiceClient.getApplicationByIdAndMaintainerCode(app, maintainerCode);
    }
}
