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
package com.lgi.appstorebundle.external.asms;

import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.common.r4j.AsmsClientInvoker;
import com.lgi.appstorebundle.external.asms.exception.AppstoreMetadataServiceClientException;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class AppstoreMetadataServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(AppstoreMetadataServiceClient.class);

    private static final String ABOUT_TO_GET_APPLICATION_METADATA_BY_APP_ID_MSG_FMT =
            "About to request AppStore Metadata Service for application id: '{}' and version: '{}'";
    private static final String ABOUT_TO_GET_APPLICATION_METADATA_BY_MAINTAINER_CODE_MSG_FMT =
            "About to request AppStore Metadata Service for application id: '{}', version: '{}' and maintainerCode: '{}'";
    private static final String SUCCESSFUL_CALL_TO_ASMS_LOG_MESSAGE = "Successfully executed request to AppStore Metadata Service";

    private static final String APPLICATION_ID_PATH_PARAM = "applicationId";
    private static final String PLATFORM_NAME_QUERY_PARAM = "platformName";
    private static final String FIRMWARE_VER_QUERY_PARAM = "firmwareVer";
    private static final String MAINTAINER_CODE_PARAM = "maintainerCode";
    public static final String GET_APPLICATION_BY_APP_ID_PATH = "apps/{" + APPLICATION_ID_PATH_PARAM + "}";
    public static final String GET_APPLICATION_BY_APP_ID_AND_MAINTAINER_CODE_PATH =
            "maintainers/{" + MAINTAINER_CODE_PARAM + "}/apps/{" + APPLICATION_ID_PATH_PARAM + "}";

    private final RestTemplate asmsRestTemplate;
    private final  AsmsClientInvoker clientInvoker;

    public AppstoreMetadataServiceClient(RestTemplate asmsRestTemplate, AsmsClientInvoker clientInvoker) {
        this.asmsRestTemplate = asmsRestTemplate;
        this.clientInvoker = clientInvoker;
    }

    public Optional<ApplicationMetadata> getApplicationByAppId(ApplicationParams appParams) {
        LOG.info(ABOUT_TO_GET_APPLICATION_METADATA_BY_APP_ID_MSG_FMT, appParams.getApplicationId(), appParams.getAppVersion());
        return clientInvoker.invoke(() -> getApplicationMetadataById(appParams));
    }

    private Optional<ApplicationMetadata> getApplicationMetadataById(ApplicationParams appParams) {
        ResponseEntity<ApplicationMetadata> response;

        Map<String, String> parameters = new HashMap<>();
        parameters.put(APPLICATION_ID_PATH_PARAM, appParams.getFullyQualifiedApplicationName());

        String urlTemplate = UriComponentsBuilder.fromUriString(GET_APPLICATION_BY_APP_ID_PATH)
                .queryParam(PLATFORM_NAME_QUERY_PARAM, appParams.getPlatformName())
                .queryParam(FIRMWARE_VER_QUERY_PARAM, appParams.getFirmwareVersion())
                .encode()
                .toUriString();

        URI uriToCall = asmsRestTemplate.getUriTemplateHandler().expand(urlTemplate, parameters);

        try {
            response = asmsRestTemplate.getForEntity(
                    uriToCall,
                    ApplicationMetadata.class
            );
        } catch (HttpStatusCodeException e) {
            return handleException(e);
        }
        LOG.info(SUCCESSFUL_CALL_TO_ASMS_LOG_MESSAGE);
        return Optional.ofNullable(response.getBody());
    }

    public Optional<ApplicationMetadataForMaintainer> getApplicationByIdAndMaintainerCode(ApplicationParams appParams, String maintainerCode) {
        LOG.info(ABOUT_TO_GET_APPLICATION_METADATA_BY_MAINTAINER_CODE_MSG_FMT, appParams.getApplicationId(), appParams.getAppVersion(), maintainerCode);
        return clientInvoker.invoke(() -> getApplicationByMaintainerAndAppId(appParams, maintainerCode));
    }

    private Optional<ApplicationMetadataForMaintainer> getApplicationByMaintainerAndAppId(ApplicationParams applicationParams,
                                                                                          String maintainerCode) {
        ResponseEntity<ApplicationMetadataForMaintainer> response;

        Map<String, String> parameters = new HashMap<>();
        parameters.put(MAINTAINER_CODE_PARAM, maintainerCode);
        parameters.put(APPLICATION_ID_PATH_PARAM, applicationParams.getFullyQualifiedApplicationName());

        String urlTemplate = UriComponentsBuilder.fromUriString(GET_APPLICATION_BY_APP_ID_AND_MAINTAINER_CODE_PATH)
                .queryParam(PLATFORM_NAME_QUERY_PARAM, applicationParams.getPlatformName())
                .queryParam(FIRMWARE_VER_QUERY_PARAM, applicationParams.getFirmwareVersion())
                .encode()
                .toUriString();

        URI uriToCall = asmsRestTemplate.getUriTemplateHandler().expand(urlTemplate, parameters);

        try {
            response = asmsRestTemplate.getForEntity(
                    uriToCall,
                    ApplicationMetadataForMaintainer.class
            );
        } catch (HttpStatusCodeException e) {
            return handleException(e);
        }
        LOG.info(SUCCESSFUL_CALL_TO_ASMS_LOG_MESSAGE);
        return Optional.ofNullable(response.getBody());
    }

    private static <T> Optional<T> handleException(HttpStatusCodeException e,
                                                   Supplier<Optional<T>> fallback) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return fallback.get();
        }
        LOG.warn("Request to AppStore Metadata Service failed, code {}, reason: {}", e.getRawStatusCode(), e.getCause());
        throw new AppstoreMetadataServiceClientException(e.getResponseBodyAsString());
    }

    private static <T> Optional<T> handleException(HttpStatusCodeException e) {
        return handleException(e, Optional::empty);
    }
}
