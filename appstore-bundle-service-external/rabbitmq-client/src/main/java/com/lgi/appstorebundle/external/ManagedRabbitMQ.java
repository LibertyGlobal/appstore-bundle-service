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
package com.lgi.appstorebundle.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgi.appstorebundle.exception.RabbitMQException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.lgi.appstorebundle.common.Headers.CORRELATION_ID;
import static java.util.Objects.requireNonNull;

@Service
public class ManagedRabbitMQ {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedRabbitMQ.class);

    private static final int PREFETCH_COUNT = 1;

    private final String generationQueueName;
    private final String encryptionQueueName;
    private Channel channel;
    private Connection connection;
    private final RabbitMQConfiguration configuration;
    private final ObjectMapper objectMapper;

    @Autowired
    public ManagedRabbitMQ(RabbitMQConfiguration configuration, ObjectMapper objectMapper) {
        this.configuration = requireNonNull(configuration);
        this.objectMapper = requireNonNull(objectMapper);
        this.generationQueueName = requireNonNull(configuration.getGenerationQueueName());
        this.encryptionQueueName = requireNonNull(configuration.getEncryptionQueueName());
    }

    @PostConstruct
    public void start() throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        LOG.info("RabbitMQ starting...");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(configuration.getUrl());
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.basicQos(PREFETCH_COUNT);
        LOG.info("RabbitMQ started.");
    }

    @PreDestroy
    public void stop() {
        try {
            LOG.info("RabbitMQ channel closing...");
            channel.close();
            LOG.info("RabbitMQ channel closed.");
            LOG.info("RabbitMQ connection closing...");
            connection.close();
            LOG.info("RabbitMQ connection closed.");
        } catch (IOException | TimeoutException e) {
            LOG.warn("RabbitMQ error during closing", e);
        }
    }

    public <T> OptionalException sendGenerationMessage(T message, String xRequestId) {
        return sendMessage(message, xRequestId, generationQueueName);
    }

    public <T> OptionalException sendEncryptionMessage(T message, String xRequestId) {
        return sendMessage(message, xRequestId, encryptionQueueName);
    }

    public <T> OptionalException sendMessage(T message, String xRequestId, String queueName) {
        final AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        builder.headers(Map.of(CORRELATION_ID, xRequestId));
        try {
            channel.basicPublish("", queueName, builder.build(), objectMapper.writeValueAsBytes(message));
            LOG.info("Message for 'x-request-id': '{}' was sent on queue: '{}'", xRequestId, queueName);
            return OptionalException.empty();
        } catch (Exception e) {
            return OptionalException.of(RabbitMQException.createDefault(xRequestId, e.getMessage()));
        }
    }

    public Channel getChannel() {
        return channel;
    }
}
