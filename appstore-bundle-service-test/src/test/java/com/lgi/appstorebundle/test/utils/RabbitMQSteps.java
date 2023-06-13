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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstorebundle.api.Environment;
import com.lgi.appstorebundle.model.EncryptionMessage;
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.lgi.appstorebundle.model.GenerationMessage;
import com.lgi.appstorebundle.test.service.model.EncryptionMessageWithEnvelope;
import com.lgi.appstorebundle.test.service.model.GenerationMessageWithEnvelope;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.qameta.allure.Step;
import org.assertj.core.api.SoftAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.TimeoutException;

public class RabbitMQSteps {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQSteps.class);

    public static final String X_REQUEST_ID = "x-request-id";

    private final ObjectMapper objectMapper;
    private final Channel generationChannel;
    private final Channel encryptionChannel;
    private final Connection connection;
    private final Stack<GenerationMessageWithEnvelope> generationMsgsStack;
    private final Stack<EncryptionMessageWithEnvelope> encryptionMsgsStack;
    private final Environment environment;

    public RabbitMQSteps(RabbitMQContainer rabbitMQContainer, ObjectMapper objectMapper, Environment environment) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.generationMsgsStack = new Stack<>();
        this.encryptionMsgsStack = new Stack<>();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitMQContainer.getAmqpUrl());
        connection = factory.newConnection();
        generationChannel = connection.createChannel();
        generationChannel.basicConsume("bundlegen-service-requests", false, createGenerationConsumer(), consumerTag -> {
        });
        encryptionChannel = connection.createChannel();
        encryptionChannel.basicConsume("bundlecrypt-service-requests", false, createEncryptionConsumer(), consumerTag -> {
        });
    }

    private DeliverCallback createGenerationConsumer() {
        return (consumerTag, delivery) -> {
            final GenerationMessage generationMessage = objectMapper.readValue(delivery.getBody(), GenerationMessage.class);
            final String xRequestId = Optional.ofNullable(delivery.getProperties().getHeaders())
                    .map(headers -> headers.get(X_REQUEST_ID))
                    .map(Object::toString)
                    .filter(maybeXRequestId -> !maybeXRequestId.isEmpty())
                    .orElse(null);
            final GenerationMessageWithEnvelope generationMessageWithEnvelope = GenerationMessageWithEnvelope.create(generationMessage, xRequestId);
            LOG.info("Message from RabbitMQContainer received: {}", generationMessageWithEnvelope);
            generationMsgsStack.push(generationMessageWithEnvelope);
        };
    }

    private DeliverCallback createEncryptionConsumer() {
        return (consumerTag, delivery) -> {
            final EncryptionMessage encryptionMessage = objectMapper.readValue(delivery.getBody(), EncryptionMessage.class);
            final String xRequestId = Optional.ofNullable(delivery.getProperties().getHeaders())
                    .map(headers -> headers.get(X_REQUEST_ID))
                    .map(Object::toString)
                    .orElseThrow();
            final EncryptionMessageWithEnvelope encryptionMessageWithEnvelope = EncryptionMessageWithEnvelope.create(encryptionMessage, xRequestId);
            LOG.info("Message from RabbitMQContainer received: {}", encryptionMessageWithEnvelope);
            encryptionMsgsStack.push(encryptionMessageWithEnvelope);
        };
    }

    @Step("Clear stack with all received messages")
    public void clearStack() {
        generationMsgsStack.removeAllElements();
        encryptionMsgsStack.removeAllElements();
    }

    @Step("Push massage to the RabbitMQ: {0}, xRequestId: {1}")
    public void pushMessage(FeedbackMessage feedbackMessage, String xRequestId) throws IOException {
        final AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        Optional.ofNullable(xRequestId).ifPresent(x -> builder.headers(Map.of(X_REQUEST_ID, x)));
        generationChannel.basicPublish("", "bundlegen-service-status", builder.build(), objectMapper.writeValueAsBytes(feedbackMessage));
    }

    @Step("Verify sent message to the RabbitMQ: {1}, xRequestId: {2}")
    public void verifySentGenerationMessage(SoftAssertions softly, GenerationMessage expected, String xRequestId) {
        softly.assertThat(generationMsgsStack.isEmpty())
                .as("GenerationMessage was not received.")
                .isFalse();
        final GenerationMessageWithEnvelope generationMessageWithEnvelope = generationMsgsStack.pop();
        final GenerationMessage generationMessage = generationMessageWithEnvelope.getGenerationMessage();
        softly.assertThat(generationMessage)
                .as("Message for requesting bundle generation not sent.")
                .isNotNull();
        softly.assertThat(generationMessage.getId())
                .as("Generation Message not contains an Id.")
                .isNotNull();
        softly.assertThat(generationMessage.getAppId())
                .as("Generation Message contains a wrong AppId.")
                .isEqualTo(expected.getAppId());
        softly.assertThat(generationMessage.getAppVersion())
                .as("Generation Message contains a wrong AppVer.")
                .isEqualTo(expected.getAppVersion());
        softly.assertThat(generationMessage.getPlatformName())
                .as("Generation Message contains a wrong PlatformName.")
                .isEqualTo(expected.getPlatformName());
        softly.assertThat(generationMessage.getFirmwareVersion())
                .as("Generation Message contains a wrong FirmwareVersion.")
                .isEqualTo(expected.getFirmwareVersion());
        softly.assertThat(generationMessage.getOciImageUrl())
                .as("Generation Message contains a wrong OciImageUrl.")
                .isEqualTo(expected.getOciImageUrl());
        softly.assertThat(generationMessage.getEncrypt())
                .as("Generation Message contains a wrong Encrypt.")
                .isEqualTo(expected.getEncrypt());
        softly.assertThat(generationMessageWithEnvelope.getXRequestId())
                .as("Generation Message contains a wrong xRequestId.")
                .isEqualTo(xRequestId);
    }

    @Step("Verify sent message to the RabbitMQ: {1}, xRequestId: {2}")
    public void verifySentEncryptionMessage(SoftAssertions softly, EncryptionMessage expected, String xRequestId) {
        softly.assertThat(encryptionMsgsStack.isEmpty())
                .as("EncryptionMessage was not received.")
                .isFalse();
        final EncryptionMessageWithEnvelope generationMessageWithEnvelope = encryptionMsgsStack.pop();
        final var encryptionMessage = generationMessageWithEnvelope.getEncryptionMessage();
        softly.assertThat(encryptionMessage)
                .as("Message for requesting bundle encryption not sent.")
                .isNotNull();
        softly.assertThat(encryptionMessage.getId())
                .as("Encryption Message contains a wrong Id.")
                .isEqualTo(expected.getId());
        softly.assertThat(encryptionMessage.getAppId())
                .as("Encryption Message contains a wrong AppId.")
                .isEqualTo(expected.getAppId());
        softly.assertThat(encryptionMessage.getAppVersion())
                .as("Encryption Message contains a wrong AppVer.")
                .isEqualTo(expected.getAppVersion());
        softly.assertThat(encryptionMessage.getPlatformName())
                .as("Encryption Message contains a wrong PlatformName.")
                .isEqualTo(expected.getPlatformName());
        softly.assertThat(encryptionMessage.getFirmwareVersion())
                .as("Encryption Message contains a wrong FirmwareVersion.")
                .isEqualTo(expected.getFirmwareVersion());
        softly.assertThat(encryptionMessage.getOciBundleUrl())
                .as("Encryption Message contains a wrong OciBundleUrl.")
                .isEqualTo(expected.getOciBundleUrl());
        softly.assertThat(generationMessageWithEnvelope.getXRequestId())
                .as("Encryption Message contains a wrong xRequestId.")
                .isEqualTo(xRequestId);
        softly.assertThat(encryptionMessage.getEnvironment())
                .as("Encryption Message contains a wrong environment.")
                .isEqualTo(environment.toString());
    }

    @Step("Verify not sent message to the RabbitMQ")
    public void verifyNotSentGenerationMessage(SoftAssertions softly) {
        final boolean messageNotReceived = generationMsgsStack.empty();
        softly.assertThat(messageNotReceived)
                .as("Generation Message for requesting bundle generation was sent but should not be.")
                .isTrue();
    }

    public void stop() {
        try {
            LOG.info("RabbitMQContainer channel closing...");
            generationChannel.close();
            encryptionChannel.close();
            LOG.info("RabbitMQContainer channel closed.");
            LOG.info("RabbitMQContainer connection closing...");
            connection.close();
            LOG.info("RabbitMQContainer connection closed.");
        } catch (IOException | TimeoutException e) {
            LOG.warn("RabbitMQContainer error during closing", e);
        }
    }
}