package com.nelumbo.park.service.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.dto.response.QueueMessageResponse;
import com.nelumbo.park.exception.exceptions.BackoffExecutionFailedException;
import com.nelumbo.park.exception.exceptions.RabbitMQConnectionException;
import com.nelumbo.park.exception.exceptions.RabbitMQConsumerException;
import com.nelumbo.park.exception.exceptions.RabbitMQMessagePublishException;
import com.nelumbo.park.utils.BackoffExecutor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class RabbitMQService implements DisposableBean {

    private static final String QUEUE_RETRY = "email_retry_queue";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RabbitMQService.class);

    private Connection connection;
    private Channel channel;

    @Value("${rabbitmq.url}")
    private String rabbitUrl;

    @Value("${queue.name}")
    private String defaultQueueName;

    @Value("${queue.final.name}")
    private String defaultFinalQueueName;

    private final ObjectMapper objectMapper;

    public RabbitMQService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Extracted for testability
    protected ConnectionFactory createConnectionFactory() {
        return new ConnectionFactory();
    }

    public void connect(String queueNameOverride) {
        String queueName = queueNameOverride != null ? queueNameOverride : defaultQueueName;
        String dlqFinalName = String.format("%s%s", queueName, defaultFinalQueueName);

        ConnectionFactory factory = createConnectionFactory();

        try {
            factory.setUri(rabbitUrl);

            if (factory.getVirtualHost() == null || factory.getVirtualHost().isEmpty()) {
                factory.setVirtualHost("/");
            }

            factory.setConnectionTimeout(30000);
            factory.setRequestedHeartbeat(60);
            factory.setNetworkRecoveryInterval(5000);
            factory.setAutomaticRecoveryEnabled(true);

            connection = factory.newConnection();
            channel = connection.createChannel();

            // All queue declarations are now within this single try block
            try {
                channel.queueDeclarePassive(queueName);
            } catch (IOException e) {
                // If passive declaration fails, it means the queue doesn't exist or there's a connection issue.
                // In either case, we proceed to declare the queues actively.
                // Any IOException during these declarations will be caught by the outer IOException catch.
                channel.queueDeclare(dlqFinalName, true, false, false, null);

                Map<String, Object> retryArgs = new HashMap<>();
                retryArgs.put("x-message-ttl", 60000);
                retryArgs.put("x-dead-letter-exchange", "");
                retryArgs.put("x-dead-letter-routing-key", queueName);

                channel.queueDeclare(QUEUE_RETRY, true, false, false, retryArgs);

                Map<String, Object> mainArgs = new HashMap<>();
                mainArgs.put("x-dead-letter-exchange", "");
                mainArgs.put("x-dead-letter-routing-key", QUEUE_RETRY);

                channel.queueDeclare(queueName, true, false, false, mainArgs);
            }
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Error de configuración al conectar a RabbitMQ: {}", e.getMessage());
            throw new RabbitMQConnectionException("Error de configuración al conectar a RabbitMQ", e);
        } catch (IOException | TimeoutException e) {
            logger.error("Error estableciendo conexión o declarando colas de RabbitMQ: {}", e.getMessage());
            throw new RabbitMQConnectionException("Error estableciendo conexión o declarando colas de RabbitMQ", e);
        }
    }

    public void sendToQueue(String queueName, Object message) {
        if (channel == null) connect(null);
        try {
            byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBytes);
        } catch (IOException e) {
            throw new RabbitMQMessagePublishException(String.format("Error publicando mensaje en la cola: %s", queueName), e);
        }
    }

    public void publishToDeadLetterQueue(QueueMessageResponse message) {
        sendToQueue(QUEUE_RETRY, message);
    }

    public void publishToFinalDLQ(QueueMessageResponse message) {
        message.setFailedAt(new Date());
        message.setFinalFailure(true);
        sendToQueue(String.format("%s%s", defaultQueueName, defaultFinalQueueName), message);
    }

    public void consumeFromQueue(String queueName, Consumer<QueueMessageResponse> callback) {
        if (channel == null) connect(null);

        try {
            channel.basicConsume(queueName, false, (consumerTag, delivery) -> {
                try {
                    QueueMessageResponse msg = objectMapper.readValue(delivery.getBody(), QueueMessageResponse.class);
                    callback.accept(msg);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException e) {
                    logger.error("Error processing message: {}", e.getMessage());
                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    } catch (IOException nackException) {
                        logger.error("Error enviando NACK: {}", nackException.getMessage());
                    }
                }
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RabbitMQConsumerException(String.format("Error configurando consumidor para la cola: %s", queueName), e);
        }
    }

    public void consumeFromFinalDLQ(Consumer<QueueMessageResponse> callback) {
        consumeFromQueue(String.format("%s%s", defaultQueueName, defaultFinalQueueName), callback);
    }

    public void publishMessageBackoff(Object message) {
        try {
            BackoffExecutor<Object> executor = new BackoffExecutor<>(200, 5, // Fast retries for testing
                    this::publishMessageInternal,
                    (error, msg) -> logger.error("Final error publishing message after retries: {}", error.getMessage()),
                    (result, msg) -> logger.info("Published message with backoff successfully."),
                    (error, msg) -> logger.warn("Retrying message publish due to: {}", error.getMessage())
            );
            executor.executeBackoff(message);
        } catch (BackoffExecutionFailedException e) {
            throw new RabbitMQMessagePublishException("Fallo final al publicar mensaje con backoff", e);
        }
    }

    private boolean publishMessageInternal(Object message) {
        try {
            if (channel == null || !channel.isOpen() || connection == null || !connection.isOpen()) {
                logger.warn("Connection or channel is not open. Attempting to reconnect...");
                connect(null);
            }
            byte[] messageBytes = objectMapper.writeValueAsBytes(message);
            channel.basicPublish("", defaultQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBytes);
            return true;
        } catch (Exception e) { // Catch broader exceptions during publish/connect
            logger.warn("Failed to publish message: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void destroy() {
        try {
            if (channel != null) channel.close();
        } catch (IOException | TimeoutException e) {
            logger.warn("Error cerrando canal de RabbitMQ: {}", e.getMessage());
        }

        try {
            if (connection != null) connection.close();
        } catch (IOException e) {
            logger.warn("Error cerrando conexión de RabbitMQ: {}", e.getMessage());
        }
    }
}
