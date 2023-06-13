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
package com.lgi.appstorebundle.test.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Header;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Maintainer;
import io.qameta.allure.Step;
import lombok.AllArgsConstructor;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@AllArgsConstructor
public class ASMSMockSteps {

    private static final String SEMICOLON_URL_ENC = "%3A";

    @Autowired
    private WireMockServer asmsWiremock;

    @Autowired
    private ObjectMapper objectMapper;

    @Step("Stub Appstore Metadata Service for getting a application metadata with status code NOT_FOUND for AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}")
    public void stubASMSWithNotFoundForApplicationMetadata(String appId, String appVer, String platformName, String firmwareVersion) {
        asmsWiremock.stubFor(get(urlPathEqualTo("/apps/" + appId + ":" + appVer))
                .withQueryParam("firmwareVer", equalTo(firmwareVersion))
                .withQueryParam("platformName", equalTo(platformName))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));
    }

    @Step("Stub Appstore Metadata Service for getting a application metadata for maintainer with status code NOT_FOUND for AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}, MaintainerCode: {4}")
    public void stubASMSWithNotFoundForApplicationMetadataForMaintainer(String appId, String appVer, String platformName, String firmwareVersion, String maintainerCode) {
        asmsWiremock.addStubMapping(get(urlPathEqualTo("/maintainers/" + maintainerCode + "/apps/" + appId + ":" + appVer))
                .withQueryParam("firmwareVer", equalTo(firmwareVersion))
                .withQueryParam("platformName", equalTo(platformName))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_NOT_FOUND)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())).build());
    }

    @Step("Stub Appstore Metadata Service for getting a application metadata with status code SC_OK for AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}")
    public void stubASMSWithApplicationMetadataInResponseBody(String appId, String appVer, String platformName, String firmwareVersion, String maintainerCode, String appName, String appUrl) throws JsonProcessingException {
        ApplicationMetadata expected = ApplicationMetadata.create(Header.create(appId, appName, appVer, appUrl), Maintainer.create(maintainerCode));
        asmsWiremock.stubFor(get(urlPathEqualTo("/apps/" + appId + SEMICOLON_URL_ENC + appVer))
                .withQueryParam("firmwareVer", equalTo(firmwareVersion))
                .withQueryParam("platformName", equalTo(platformName))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(expected))
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));
    }

    @Step("Stub Appstore Metadata Service for getting a application metadata for maintainer with status code SC_OK for AppId: {0}, AppVer: {1}, PlatformName: {2}, FirmwareVer: {3}, MaintainerCode: {4}")
    public void stubASMSWithApplicationMetadataForMaintainerInResponseBody(String appId, String appVer, String platformName, String firmwareVersion, String maintainerCode, String appName, String appUrl, String ociImageUrl) throws JsonProcessingException {
        ApplicationMetadataForMaintainer expected = ApplicationMetadataForMaintainer.create(HeaderForMaintainer.create(appId, appName, appVer, appUrl, ociImageUrl));
        asmsWiremock.addStubMapping(get(urlPathEqualTo("/maintainers/" + maintainerCode + "/apps/" + appId + SEMICOLON_URL_ENC + appVer))
                .withQueryParam("firmwareVer", equalTo(firmwareVersion))
                .withQueryParam("platformName", equalTo(platformName))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(expected))
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())).build());
    }

    @Step("Reset all stubs for Appstore Metadata Service")
    public void resetAll() {
        asmsWiremock.resetAll();
    }
}