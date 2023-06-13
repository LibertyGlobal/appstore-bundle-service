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
package com.lgi.appstorebundle.configuration;

import com.google.common.base.Suppliers;
import com.lgi.appstorebundle.api.model.BundleStatus;
import com.lgi.appstorebundle.external.ManagedRabbitMQ;
import com.lgi.appstorebundle.external.RabbitMQConfiguration;
import com.lgi.appstorebundle.external.RabbitMQConsumer;
import com.lgi.appstorebundle.model.FeedbackMessage;
import com.lgi.appstorebundle.service.BundleService;
import com.lgi.appstorebundle.util.ConsumerFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Configuration
public class RabbitMQConsumersConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumersConfiguration.class);
    private static final boolean AUTO_ACKNOWLEDGE = false;

    @Value("${bundle.encryption.enabled}")
    private boolean encrypt;

    @Autowired
    private RabbitMQConfiguration rabbitMQConfiguration;

    @Autowired
    private ConsumerFactory consumerFactory;

    @Autowired
    private BundleService bundleService;

    @Autowired
    private ManagedRabbitMQ managedRabbitMQ;

    @PostConstruct
    public void setUpConsumers() throws IOException {
        List<RabbitMQConsumer<RabbitMQConfiguration>> rabbitMQConsumers = List.of(
                new RabbitMQConsumer<>(
                        RabbitMQConfiguration::getGenerationStatusQueueName,
                        Suppliers.memoize(() -> consumerFactory.createConsumer(this::processGenerationStatus))
                ),
                new RabbitMQConsumer<>(
                        RabbitMQConfiguration::getEncryptionStatusQueueName,
                        Suppliers.memoize(() -> consumerFactory.createConsumer(this::processEncryptionStatus))
                )
        );

        for (RabbitMQConsumer<RabbitMQConfiguration> rabbitMqConsumer : rabbitMQConsumers) {
            Channel channel = managedRabbitMQ.getChannel();
            channel.basicConsume(
                    rabbitMqConsumer.getQueueNameSupplier().apply(rabbitMQConfiguration),
                    AUTO_ACKNOWLEDGE,
                    decorateBySendingAck(rabbitMqConsumer, channel),
                    consumerTag -> {}
            );
        }
    }

    private DeliverCallback decorateBySendingAck(RabbitMQConsumer<RabbitMQConfiguration> consumer, Channel channel) {
        return (consumerTag, delivery) -> {
            consumer.getConsumer().handle(consumerTag, delivery);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), AUTO_ACKNOWLEDGE);
        };
    }

    private void processGenerationStatus(FeedbackMessage feedbackMessage, String xRequestId) {
        process(feedbackMessage, xRequestId, bundleStatus -> {
            bundleService.updateBundleStatusIfNewer(feedbackMessage.getId(), bundleStatus, feedbackMessage.getMessageTimestamp());
            if (bundleStatus == BundleStatus.GENERATION_COMPLETED && encrypt) {
                bundleService.triggerBundleEncryption(feedbackMessage.getId(), xRequestId);
            }
        });
    }

    private void processEncryptionStatus(FeedbackMessage feedbackMessage, String xRequestId) {
        process(feedbackMessage, xRequestId, bundleStatus ->
                bundleService.updateBundleStatusIfNewer(feedbackMessage.getId(), bundleStatus, feedbackMessage.getMessageTimestamp()));
    }

    private void process(FeedbackMessage feedbackMessage, String
            xRequestId, Consumer<BundleStatus> bundleStatusConsumer) {
        Optional.ofNullable(feedbackMessage.getMessageTimestamp())
                .ifPresentOrElse(
                        messageTimestamp -> BundleStatus.of(feedbackMessage.getPhaseCode())
                                .ifPresentOrElse(bundleStatusConsumer,
                                        () -> LOG.warn("Message for 'x-request-id': '{}' does not have a valid 'phaseCode': '{}'. Cannot be processed.", xRequestId, feedbackMessage.getPhaseCode())),
                        () -> LOG.warn("Message for 'x-request-id': '{}' does not have a 'messageTimestamp'. Cannot be processed.", xRequestId));
    }
}
