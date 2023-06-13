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

import static java.util.Objects.requireNonNull;

public class EncryptionMessageFactory {

    private final Environment environment;
    private final String bundleExtension;

    public EncryptionMessageFactory(Environment env, String bundleExtension) {
        this.environment = requireNonNull(env, "environment");
        this.bundleExtension = requireNonNull(bundleExtension, "bundleExtension");
    }

    public EncryptionMessage fromBundle(Bundle bundle) {
        requireNonNull(bundle, "bundle");

        final ApplicationContext appCtx = bundle.getApplicationContext();
        return EncryptionMessage.create(bundle.getId(), appCtx.getApplicationId(), appCtx.getApplicationVersion(),
                appCtx.getPlatformName(), appCtx.getFirmwareVersion(), buildBundleUrl(appCtx), environment.toString());
    }

    private String buildBundleUrl(ApplicationContext appCtx) {
        requireNonNull(appCtx, "applicationContext");

        return String.format("/%s/%s/%s/%s/%s", appCtx.getApplicationId(), appCtx.getApplicationVersion(), appCtx.getPlatformName(), appCtx.getFirmwareVersion(),
                buildBundleName(appCtx));
    }

    private String buildBundleName(ApplicationContext appCtx) {
        requireNonNull(appCtx, "applicationContext");

        return String.format("%s-%s-%s-%s.%s", appCtx.getApplicationId(), appCtx.getApplicationVersion(), appCtx.getPlatformName(), appCtx.getFirmwareVersion(), bundleExtension);
    }
}
