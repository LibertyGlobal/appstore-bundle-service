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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.lgi.appstorebundle.api.ApplicationParams;
import com.lgi.appstorebundle.common.r4j.AsmsClientInvoker;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadata;
import com.lgi.appstorebundle.external.asms.model.ApplicationMetadataForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Header;
import com.lgi.appstorebundle.external.asms.model.HeaderForMaintainer;
import com.lgi.appstorebundle.external.asms.model.Maintainer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppstoreMetadataServiceClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String APP_ID = "MY_APP_ID";
    private static final String APP_VER = "MY_APP_VER";
    private static final String PLATFORM_NAME = "PLATFORM_NAME";
    private static final String FIRMWARE_VER = "FIRMWARE_VER";
    private static final String APP_BUNDLE_NAME = "APP_BUNDLE_NAME";
    private static final String MAINTAINER_CODE = "MAINTAINER_CODE";
    private static final String PLATFORM_NAME_QUERY_PARAM = "platformName";
    private static final String FIRMWARE_VER_QUERY_PARAM = "firmwareVer";
    private static final boolean ENCRYPTION_ENABLED = true;
    private static final WireMockClassRule WIREMOCK = new WireMockClassRule(Options.DYNAMIC_PORT);
    private AppstoreMetadataServiceClient appstoreMetadataServiceClient;
    private CircuitBreaker circuitBreaker;
    private static final String APP_NAME = "APP_NAME";
    private static final String APP_URL = "http://hostname/demo.id.appl/2.2/platformName/firmwareVer/demo.id.appl_2.2_platformName_firmwareVer.tar.gz";
    private static final String OCI_IMAGE_URL = "OCI_URL";

    private static final ApplicationParams VALID_APP_PARAMS =
            ApplicationParams.create(APP_ID, APP_VER, PLATFORM_NAME, FIRMWARE_VER, APP_BUNDLE_NAME);

    @AfterAll
    static void afterAll() {
        WIREMOCK.stop();
    }

    @BeforeAll
    static void beforeAll() {
        WIREMOCK.start();
    }

    @BeforeEach
    void beforeEach() {
        WIREMOCK.resetAll();
        circuitBreaker = createCircuitBreaker();
        appstoreMetadataServiceClient = new AppstoreMetadataServiceClient(buildAsmsRestTemplate(), buildClientInvoker());
    }

    @Test
    void shouldExtractApplicationMetadataByAppIdOnSuccess() throws JsonProcessingException {
        // GIVEN
        ApplicationMetadata expected = ApplicationMetadata.create(Header.create(APP_ID, APP_NAME, APP_VER, APP_URL), Maintainer.create(MAINTAINER_CODE));

        WIREMOCK.stubFor(get(new UrlPattern(containing("/apps/" + APP_ID + "%3A" + APP_VER), false))
                .withQueryParam(PLATFORM_NAME_QUERY_PARAM, equalTo(PLATFORM_NAME))
                .withQueryParam(FIRMWARE_VER_QUERY_PARAM, equalTo(FIRMWARE_VER))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(expected))
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())));

        // WHEN
        ApplicationMetadata actual = appstoreMetadataServiceClient.getApplicationByAppId(VALID_APP_PARAMS).orElseThrow();

        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnEmptyOptionalWhenApplicationWithAppIdNotFound() {
        // GIVEN
        WIREMOCK.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody("")
                        .withStatus(HttpStatus.SC_NOT_FOUND)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())));

        // WHEN
        Optional<ApplicationMetadata> maybeApplicationMetadata = appstoreMetadataServiceClient.getApplicationByAppId(VALID_APP_PARAMS);

        // THEN
        assertTrue(maybeApplicationMetadata.isEmpty());
    }

    @Test
    void shouldCloseCircuitWhenFailureThresholdPassedOnGetApplicationByAppId() {
        // GIVEN
        WIREMOCK.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        assertSame(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        // WHEN
        for (int i = 0; i < 2; i++) {
            try {
                appstoreMetadataServiceClient.getApplicationByAppId(VALID_APP_PARAMS);
            } catch (Exception ex) {
                // consume exception
            }
        }

        // WHEN
        assertSame(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    void shouldExtractApplicationMetadataByMaintainerCodeOnSuccess() throws JsonProcessingException {
        // GIVEN
        ApplicationMetadataForMaintainer expected =
                ApplicationMetadataForMaintainer.create(HeaderForMaintainer.create(APP_ID, APP_NAME, APP_VER, APP_URL, ENCRYPTION_ENABLED, OCI_IMAGE_URL));

        WIREMOCK.stubFor(get(new UrlPattern(containing("/maintainers/" + MAINTAINER_CODE +"/apps/" + APP_ID + "%3A" + APP_VER), false))
                .withQueryParam(PLATFORM_NAME_QUERY_PARAM, equalTo(PLATFORM_NAME))
                .withQueryParam(FIRMWARE_VER_QUERY_PARAM, equalTo(FIRMWARE_VER))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(expected))
                        .withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())));

        // WHEN
        ApplicationMetadataForMaintainer actual =
                appstoreMetadataServiceClient.getApplicationByIdAndMaintainerCode(VALID_APP_PARAMS, MAINTAINER_CODE).get();

        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnEmptyOptionalWhenApplicationWithMaintainerCodeNotFound() {
        // GIVEN
        WIREMOCK.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withBody("")
                        .withStatus(HttpStatus.SC_NOT_FOUND)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())));

        // WHEN
        Optional<ApplicationMetadataForMaintainer> maybeApplicationMetadata =
                appstoreMetadataServiceClient.getApplicationByIdAndMaintainerCode(VALID_APP_PARAMS, MAINTAINER_CODE);

        // THEN
        assertTrue(maybeApplicationMetadata.isEmpty());
    }

    @Test
    void shouldCloseCircuitWhenFailureThresholdPassedOnGetApplicationByMaintainerCode() {
        // GIVEN
        WIREMOCK.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        assertSame(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        // WHEN
        for (int i = 0; i < 2; i++) {
            try {
                appstoreMetadataServiceClient.getApplicationByIdAndMaintainerCode(VALID_APP_PARAMS, MAINTAINER_CODE);
            } catch (Exception ex) {
                // consume exception
            }
        }

        // WHEN
        assertSame(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    private RestTemplate buildAsmsRestTemplate() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        return builder
                .uriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + WIREMOCK.port()))
                .messageConverters(new MappingJackson2HttpMessageConverter())
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }

    private AsmsClientInvoker buildClientInvoker() {
        return new AsmsClientInvoker(circuitBreaker, null);
    }

    private static CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(100)
                .ringBufferSizeInClosedState(2)
                .ringBufferSizeInHalfOpenState(2)
                .build();
        return CircuitBreaker.of("test circuit breaker name", circuitBreakerConfig);
    }
}