package com.nelumbo.park.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.model.QueueMessage;
import com.nelumbo.park.utils.BackoffExecutor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class RabbitMQService implements DisposableBean {

    private static final String QUEUE_RETRY = "email_retry_queue";

    private Connection connection;
    private Channel channel;

    @Value("${rabbitmq.url}")
    private String rabbitUrl;

    @Value("${queue.name}")
    private String defaultQueueName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void connect(String queueNameOverride) throws Exception {
        String queueName = queueNameOverride != null ? queueNameOverride : defaultQueueName;
        String dlqFinalName = queueName + ".dlq.final";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(rabbitUrl);
        connection = factory.newConnection();
        channel = connection.createChannel();

        try {
            channel.queueDeclarePassive(queueName);
        } catch (IOException e) {
            channel.queueDeclare(dlqFinalName, true, false, false, null);

            Map<String, Object> retryArgs = new HashMap<>();
            retryArgs.put("x-message-ttl", 60000);
            retryArgs.put("x-dead-letter-exchange", "");
            retryArgs.put("x-dead-letter-routing-key", queueName);

            channel.queueDeclare(QUEUE_RETRY, true, false, false, retryArgs);

            Map<String, Object> mainArgs = new HashMap<>();
            mainArgs.put("x-dead-letter-exchange", "");
            mainArgs.put("x-dead-letter-routing-key", queueName);

            channel.queueDeclare(queueName, true, false, false, mainArgs);
            System.out.println("Created queues DLQ Final: " + e.getMessage());
        }
    }

    public void sendToQueue(String queueName, Object message) throws Exception {
        if (channel == null) connect(null);
        byte[] messageBytes = objectMapper.writeValueAsBytes(message);
        channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBytes);
    }

    public void publishToDeadLetterQueue(QueueMessage message) throws Exception {
        sendToQueue(QUEUE_RETRY, message);
    }

    public void publishToFinalDLQ(QueueMessage message) throws Exception {
        message.setFailedAt(new Date());
        message.setFinalFailure(true);
        sendToQueue(defaultQueueName + ".dlq.final", message);
    }

    public void consumeFromQueue(String queueName, Consumer<QueueMessage> callback) throws Exception {
        if (channel == null) connect(null);

        channel.basicConsume(queueName, false, (consumerTag, delivery) -> {
            try {
                QueueMessage msg = objectMapper.readValue(delivery.getBody(), QueueMessage.class);
                callback.accept(msg);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        }, consumerTag -> {});
    }

    public void consumeFromFinalDLQ(Consumer<QueueMessage> callback) throws Exception {
        consumeFromQueue(defaultQueueName + ".dlq.final", callback);
    }

    public void publishMessageBackoff(Object message) throws Exception {
        BackoffExecutor<Object> executor = new BackoffExecutor<>(1000, 15,
                msg -> {
                    try {
                        return this._publishMessageInternal(msg);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                (error, msg) -> System.err.println("Final error: " + error.getMessage()),
                (result, msg) -> System.out.println("Published with backoff"),
                (error, msg) -> System.out.println("Retrying due to: " + error.getMessage())
        );
        executor.execute(message);
    }

    private boolean _publishMessageInternal(Object message) throws Exception {
        if (channel == null) connect(null);
        byte[] messageBytes = objectMapper.writeValueAsBytes(message);
        channel.basicPublish("", defaultQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBytes);
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}
