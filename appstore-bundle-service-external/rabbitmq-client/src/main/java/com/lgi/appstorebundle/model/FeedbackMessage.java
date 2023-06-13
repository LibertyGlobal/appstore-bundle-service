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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.springframework.lang.Nullable;
import java.util.UUID;

@AutoValue
public abstract class FeedbackMessage {

    public abstract UUID getId();

    public abstract String getPhaseCode();

    @Nullable
    @JsonSerialize(using = DateTimeSerializer.class)
    public abstract DateTime getMessageTimestamp();

    @Nullable
    public abstract ErrorMessage getError();

    @JsonCreator
    public static FeedbackMessage create(UUID id, String phaseCode, DateTime messageTimestamp, ErrorMessage error) {
        return new AutoValue_FeedbackMessage(id, phaseCode, messageTimestamp, error);
    }
}

