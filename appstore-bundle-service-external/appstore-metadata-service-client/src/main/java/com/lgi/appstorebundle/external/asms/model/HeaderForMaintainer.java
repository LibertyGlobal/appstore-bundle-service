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
package com.lgi.appstorebundle.external.asms.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class HeaderForMaintainer {

    public abstract String getId();

    public abstract String getName();

    public abstract String getVersion();

    public abstract String getUrl();

    @Nullable
    public abstract Boolean getEncryption();

    public abstract String getOciImageUrl();

    @JsonCreator
    public static HeaderForMaintainer create(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("version") String version,
            @JsonProperty("url") String url,
            @JsonProperty("encryption") Boolean encryption,
            @JsonProperty("ociImageUrl") String ociImageUrl) {
        return new AutoValue_HeaderForMaintainer(id, name, version, url, encryption, ociImageUrl);
    }
}
