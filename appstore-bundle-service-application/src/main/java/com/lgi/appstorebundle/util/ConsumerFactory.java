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
package com.lgi.appstorebundle.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstorebundle.external.RabbitMQConsumerMDC;
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.lgi.appstorebundle.common.Headers.CORRELATION_ID;

@Component
public class ConsumerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerFactory.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public ConsumerFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeliverCallback createConsumer(BiConsumer<FeedbackMessage, String> messageConsumer) {
        return (consumerTag, delivery) -> {
            try {
                consumeMessage(delivery, messageConsumer);
            } catch (Exception e) {
                LOG.error("Received message cannot be processed. Exception: '{}'.", e.getMessage());
            } finally {
                RabbitMQConsumerMDC.clear();
            }
        };
    }

    private void consumeMessage(Delivery delivery, BiConsumer<FeedbackMessage, String> messageConsumer) {
        final Optional<String> maybeXRequestId = getXRequestIdFromReceivedMessage(delivery);
        maybeXRequestId.ifPresentOrElse(xRequestId ->
                {
                    RabbitMQConsumerMDC.populate(xRequestId);
                    Optional<FeedbackMessage> maybeFeedbackMessage = getFeedbackMessage(delivery);
                    maybeFeedbackMessage.ifPresent(feedbackMessage -> {
                        logMessage(feedbackMessage);
                        messageConsumer.accept(feedbackMessage, xRequestId);
                    });
                },
                () -> LOG.warn("Received message does not have an 'x-request-id'. Cannot be processed.")
        );
    }

    private Optional<String> getXRequestIdFromReceivedMessage(Delivery delivery) {
        return Optional.ofNullable(delivery.getProperties().getHeaders())
                .map(headers -> headers.get(CORRELATION_ID))
                .map(Object::toString)
                .filter(xRequestId -> !xRequestId.isEmpty());
    }

    private Optional<FeedbackMessage> getFeedbackMessage(Delivery delivery) {
        try {
            return Optional.ofNullable(objectMapper.readValue(delivery.getBody(), FeedbackMessage.class));
        } catch (IOException exception) {
            LOG.error("Received message cannot be parsed to FeedbackMessage. Exception: '{}'.", exception.getMessage());
            return Optional.empty();
        }
    }

    private void logMessage(FeedbackMessage feedbackMessage) {
        Optional.ofNullable(feedbackMessage.getError())
                .ifPresentOrElse(
                        errorMessage -> LOG.warn("Received message contains 'ErrorCode': '{}' and 'ErrorMessage': '{}'", errorMessage.getCode(), errorMessage.getMessage()),
                        () -> LOG.info("Received a message with 'phaseCode': '{}'.", feedbackMessage.getPhaseCode()));
    }
}