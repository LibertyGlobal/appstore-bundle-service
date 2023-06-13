--
-- If not stated otherwise in this file or this component's LICENSE file the
-- following copyright and licenses apply:
--
-- Copyright 2023 Liberty Global Technology Services BV
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE bundle (
    id UUID NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    application_id VARCHAR(255) NOT NULL,
    application_version VARCHAR(255) NOT NULL,
    platform_name VARCHAR(255) NOT NULL,
    firmware_version VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('GENERATION_REQUESTED', 'GENERATION_LAUNCHED', 'GENERATION_COMPLETED', 'ENCRYPTION_REQUESTED', 'ENCRYPTION_LAUNCHED', 'ENCRYPTION_COMPLETED', 'BUNDLE_ERROR')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    x_request_id VARCHAR(255) NOT NULL UNIQUE,
    message_timestamp TIMESTAMPTZ NOT NULL
);