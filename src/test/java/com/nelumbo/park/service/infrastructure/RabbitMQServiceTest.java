package com.nelumbo.park.service.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.dto.response.QueueMessageResponse;
import com.nelumbo.park.exception.exceptions.RabbitMQConnectionException;
import com.nelumbo.park.exception.exceptions.RabbitMQConsumerException;
import com.nelumbo.park.exception.exceptions.RabbitMQMessagePublishException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RabbitMQServiceTest {

    @InjectMocks
    @Spy // Use Spy to allow calling real methods while mocking others
    private RabbitMQService rabbitMQService;

    @Mock
    private Connection mockConnection;
    @Mock
    private Channel mockChannel;
    @Mock
    private ObjectMapper mockObjectMapper; // Mock ObjectMapper for writeValueAsBytes
    @Mock
    private ConnectionFactory mockConnectionFactory;

    @BeforeEach
    void setUp() throws IOException, TimeoutException {
        // Set @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(rabbitMQService, "rabbitUrl", "amqp://localhost");
        ReflectionTestUtils.setField(rabbitMQService, "defaultQueueName", "test_queue");
        ReflectionTestUtils.setField(rabbitMQService, "defaultFinalQueueName", "_final_dlq");

        // Mock createConnectionFactory to return our mockConnectionFactory
        doReturn(mockConnectionFactory).when(rabbitMQService).createConnectionFactory();

        // Mock ConnectionFactory behavior
        when(mockConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);

        // Default behavior for channel and connection
        when(mockConnection.isOpen()).thenReturn(true);
        when(mockChannel.isOpen()).thenReturn(true);
        when(mockObjectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
    }

    @Test
    @DisplayName("Should send message to queue successfully")
    void sendToQueue_Success() throws IOException {
        // Given
        String queueName = "test_queue";
        Object message = new Object(); // Any object

        // When
        rabbitMQService.sendToQueue(queueName, message);

        // Then
        verify(mockObjectMapper, times(1)).writeValueAsBytes(message);
        verify(mockChannel, times(1)).basicPublish(
                eq(""), eq(queueName), any(AMQP.BasicProperties.class), eq("{}".getBytes()));
    }

    @Test
    @DisplayName("Should connect and send message if channel is null")
    void sendToQueue_ChannelNull_ConnectsAndSends() throws IOException, TimeoutException {
        // Given
        ReflectionTestUtils.setField(rabbitMQService, "channel", null); // Simulate null channel
        String queueName = "test_queue";
        Object message = new Object();

        // When
        rabbitMQService.sendToQueue(queueName, message);

        // Then
        verify(rabbitMQService, times(1)).connect(null); // Verify connect was called
        verify(mockObjectMapper, times(1)).writeValueAsBytes(message);
        verify(mockChannel, times(1)).basicPublish(
                eq(""), eq(queueName), any(AMQP.BasicProperties.class), eq("{}".getBytes()));
    }

    @Test
    @DisplayName("Should throw RabbitMQMessagePublishException on IOException during publish")
    void sendToQueue_IOException_ThrowsException() throws IOException {
        // Given
        String queueName = "test_queue";
        Object message = new Object();
        doThrow(new IOException("Test IO Exception")).when(mockChannel).basicPublish(
                anyString(), anyString(), any(AMQP.BasicProperties.class), any(byte[].class));

        // When / Then
        RabbitMQMessagePublishException thrown = assertThrows(RabbitMQMessagePublishException.class, () -> {
            rabbitMQService.sendToQueue(queueName, message);
        });

        assertTrue(thrown.getMessage().contains("Error publicando mensaje en la cola: test_queue"));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Should throw RabbitMQMessagePublishException if all backoff attempts fail")
    void publishMessageBackoff_AllAttemptsFail_ThrowsException() throws IOException {
        // Given
        Object message = new Object();
        // Simulate a persistent failure during publishing
        doThrow(new IOException("Publishing failed consistently"))
                .when(mockChannel).basicPublish(anyString(), anyString(), any(), any());

        // When / Then
        RabbitMQMessagePublishException thrown = assertThrows(RabbitMQMessagePublishException.class, () -> {
            rabbitMQService.publishMessageBackoff(message);
        });

        // Verify
        assertTrue(thrown.getMessage().contains("Fallo final al publicar mensaje con backoff"));
        // Verify that connect and publish were attempted multiple times (initial + retries)
        verify(rabbitMQService, atLeast(5)).connect(null); // 1 initial + 5 retries
        verify(mockChannel, atLeast(5)).basicPublish(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("destroy should close channel and connection")
    void destroy_ClosesChannelAndConnection() throws IOException, TimeoutException {
        // When
        rabbitMQService.destroy();

        // Then
        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @DisplayName("destroy should handle IOException when closing channel")
    void destroy_HandlesIOException_Channel() throws IOException, TimeoutException {
        // Given
        doThrow(new IOException("Channel close error")).when(mockChannel).close();

        // When
        rabbitMQService.destroy();

        // Then
        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close(); // Connection should still attempt to close
        // No exception should be thrown from destroy
    }

    @Test
    @DisplayName("destroy should handle IOException when closing connection")
    void destroy_HandlesIOException_Connection() throws IOException, TimeoutException {
        // Given
        doThrow(new IOException("Connection close error")).when(mockConnection).close();

        // When
        rabbitMQService.destroy();

        // Then
        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close();
        // No exception should be thrown from destroy
    }

    @Test
    @DisplayName("publishToDeadLetterQueue should call sendToQueue with retry queue name")
    void publishToDeadLetterQueue_CallsSendToQueue() {
        // Given
        QueueMessageResponse message = new QueueMessageResponse();
        // Mock sendToQueue as it's called internally
        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());

        // When
        rabbitMQService.publishToDeadLetterQueue(message);

        // Then
        verify(rabbitMQService, times(1)).sendToQueue(eq("email_retry_queue"), eq(message));
    }

    @Test
    @DisplayName("publishToFinalDLQ should call sendToQueue with final DLQ name and update message")
    void publishToFinalDLQ_CallsSendToQueueAndUpdateMessage() {
        // Given
        QueueMessageResponse message = new QueueMessageResponse();
        message.setFailedAt(null); // Ensure it's null initially
        message.setFinalFailure(false); // Ensure it's false initially

        // Mock sendToQueue as it's called internally
        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());

        // When
        rabbitMQService.publishToFinalDLQ(message);

        // Then
        verify(rabbitMQService, times(1)).sendToQueue(eq("test_queue_final_dlq"), eq(message));
        assertNotNull(message.getFailedAt());
        assertTrue(message.isFinalFailure());
    }

    @Test
    @DisplayName("consumeFromQueue should set up consumer and process message successfully")
    void consumeFromQueue_Success() throws IOException {
        // Given
        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);
        QueueMessageResponse testMessage = new QueueMessageResponse();

        // Mock ObjectMapper to return the test message when reading bytes
        when(mockObjectMapper.readValue(any(byte[].class), eq(QueueMessageResponse.class)))
                .thenReturn(testMessage);

        // Capture the DeliverCallback passed to basicConsume
        ArgumentCaptor<com.rabbitmq.client.DeliverCallback> deliverCallbackCaptor =
                ArgumentCaptor.forClass(com.rabbitmq.client.DeliverCallback.class);

        // When consumeFromQueue is called
        rabbitMQService.consumeFromQueue(queueName, mockCallback);

        // Then basicConsume should be called
        verify(mockChannel, times(1)).basicConsume(
                eq(queueName), eq(false), deliverCallbackCaptor.capture(), any(com.rabbitmq.client.CancelCallback.class));

        // Simulate message delivery
        com.rabbitmq.client.DeliverCallback capturedCallback = deliverCallbackCaptor.getValue();
        com.rabbitmq.client.Envelope mockEnvelope = mock(com.rabbitmq.client.Envelope.class);
        when(mockEnvelope.getDeliveryTag()).thenReturn(123L);
        com.rabbitmq.client.Delivery mockDelivery = new com.rabbitmq.client.Delivery(
                mockEnvelope, mock(AMQP.BasicProperties.class), "{}".getBytes()); // Corrected: removed "consumerTag"

        capturedCallback.handle("consumerTag", mockDelivery);

        // Verify callback was called and message was acknowledged
        verify(mockCallback, times(1)).accept(testMessage);
        verify(mockChannel, times(1)).basicAck(123L, false);
        verify(mockChannel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("consumeFromQueue should handle IOException during message processing and nack message")
    void consumeFromQueue_IOException_NacksMessage() throws IOException {
        // Given
        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);

        // Mock ObjectMapper to throw IOException when reading bytes
        when(mockObjectMapper.readValue(any(byte[].class), eq(QueueMessageResponse.class)))
                .thenThrow(new IOException("Error parsing message"));

        // Capture the DeliverCallback passed to basicConsume
        ArgumentCaptor<com.rabbitmq.client.DeliverCallback> deliverCallbackCaptor =
                ArgumentCaptor.forClass(com.rabbitmq.client.DeliverCallback.class);

        // When consumeFromQueue is called
        rabbitMQService.consumeFromQueue(queueName, mockCallback);

        // Then basicConsume should be called
        verify(mockChannel, times(1)).basicConsume(
                eq(queueName), eq(false), deliverCallbackCaptor.capture(), any(com.rabbitmq.client.CancelCallback.class));

        // Simulate message delivery
        com.rabbitmq.client.DeliverCallback capturedCallback = deliverCallbackCaptor.getValue();
        com.rabbitmq.client.Envelope mockEnvelope = mock(com.rabbitmq.client.Envelope.class);
        when(mockEnvelope.getDeliveryTag()).thenReturn(456L);
        com.rabbitmq.client.Delivery mockDelivery = new com.rabbitmq.client.Delivery(
                mockEnvelope, mock(AMQP.BasicProperties.class), "invalid_bytes".getBytes()); // Corrected: removed "consumerTag"

        capturedCallback.handle("consumerTag", mockDelivery);

        // Verify callback was NOT called and message was nacked
        verify(mockCallback, never()).accept(any());
        verify(mockChannel, never()).basicAck(anyLong(), anyBoolean());
        verify(mockChannel, times(1)).basicNack(456L, false, false);
    }

    @Test
    @DisplayName("consumeFromQueue should throw RabbitMQConsumerException on IOException during basicConsume setup")
    void consumeFromQueue_BasicConsumeIOException_ThrowsException() throws IOException {
        // Given
        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);
        doThrow(new IOException("Basic consume setup error")).when(mockChannel).basicConsume(
                anyString(), anyBoolean(), any(com.rabbitmq.client.DeliverCallback.class), any(com.rabbitmq.client.CancelCallback.class));

        // When / Then
        RabbitMQConsumerException thrown = assertThrows(RabbitMQConsumerException.class, () -> {
            rabbitMQService.consumeFromQueue(queueName, mockCallback);
        });

        assertTrue(thrown.getMessage().contains("Error configurando consumidor para la cola: test_consumer_queue"));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("consumeFromFinalDLQ should call consumeFromQueue with final DLQ name")
    void consumeFromFinalDLQ_CallsConsumeFromQueue() {
        // Given
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);
        // Mock consumeFromQueue as it's called internally
        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());

        // When
        rabbitMQService.consumeFromFinalDLQ(mockCallback);

        // Then
        verify(rabbitMQService, times(1)).consumeFromQueue(eq("test_queue_final_dlq"), eq(mockCallback));
    }

    @Test
    @DisplayName("connect should establish connection and declare queues if not passive")
    void connect_EstablishesConnectionAndDeclaresQueues() throws Exception {
        // Given
        // Simulate queueDeclarePassive throwing IOException, which triggers queueDeclare logic
        doThrow(new IOException("Queue not found")).when(mockChannel).queueDeclarePassive(anyString());

        // When connect is called
        rabbitMQService.connect(null);

        // Then
        // Verify that queueDeclarePassive was attempted
        verify(mockChannel, times(1)).queueDeclarePassive(eq("test_queue"));

        // Verify that queueDeclare for dlqFinalName, QUEUE_RETRY, and main queue were called
        // because queueDeclarePassive threw an exception.
        verify(mockChannel, times(1)).queueDeclare(eq("test_queue_final_dlq"), eq(true), eq(false), eq(false), isNull());

        ArgumentCaptor<Map<String, Object>> retryArgsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockChannel, times(1)).queueDeclare(eq("email_retry_queue"), eq(true), eq(false), eq(false), retryArgsCaptor.capture());
        Map<String, Object> retryArgs = retryArgsCaptor.getValue();
        assertEquals(60000, retryArgs.get("x-message-ttl"));
        assertEquals("", retryArgs.get("x-dead-letter-exchange"));
        assertEquals("test_queue", retryArgs.get("x-dead-letter-routing-key"));

        ArgumentCaptor<Map<String, Object>> mainArgsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockChannel, times(1)).queueDeclare(eq("test_queue"), eq(true), eq(false), eq(false), mainArgsCaptor.capture());
        Map<String, Object> mainArgs = mainArgsCaptor.getValue();
        assertEquals("", mainArgs.get("x-dead-letter-exchange"));
        assertEquals("email_retry_queue", mainArgs.get("x-dead-letter-routing-key"));
    }

    @Test
    @DisplayName("connect should not declare queues if queueDeclarePassive succeeds")
    void connect_QueueDeclarePassiveSuccess_NoQueueDeclarations() throws Exception {
        // Given
        // Simulate queueDeclarePassive succeeding
        when(mockChannel.queueDeclarePassive(anyString())).thenReturn(mock(AMQP.Queue.DeclareOk.class));

        // When connect is called
        rabbitMQService.connect(null);

        // Then
        verify(mockChannel, times(1)).queueDeclarePassive(eq("test_queue"));
        // Verify that no other queueDeclare calls were made
        verify(mockChannel, never()).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @DisplayName("connect should throw RabbitMQConnectionException if newConnection fails")
    void connect_NewConnectionFails_ThrowsException() throws IOException, TimeoutException {
        // Given
        // Simulate newConnection throwing IOException
        doThrow(new IOException("Connection failed")).when(mockConnectionFactory).newConnection();

        // When / Then
        RabbitMQConnectionException thrown = assertThrows(RabbitMQConnectionException.class, () -> {
            rabbitMQService.connect(null);
        });

        assertTrue(thrown.getMessage().contains("Error estableciendo conexión o declarando colas de RabbitMQ"));
        assertTrue(thrown.getCause() instanceof IOException);

        // Verify that newConnection was attempted
        verify(mockConnectionFactory, times(1)).newConnection();
        // Verify that no channel operations were attempted
        verify(mockChannel, never()).queueDeclarePassive(anyString());
        verify(mockChannel, never()).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @DisplayName("connect should throw RabbitMQConnectionException if queue declaration fails")
    void connect_QueueDeclarationFails_ThrowsRabbitMQConnectionException() throws IOException, TimeoutException {
        // Given
        // Simulate queueDeclarePassive throwing IOException
        doThrow(new IOException("Passive declare failed")).when(mockChannel).queueDeclarePassive(anyString());
        // Simulate queueDeclare for the final DLQ throwing IOException
        doThrow(new IOException("Final DLQ declare failed")).when(mockChannel).queueDeclare(
                eq("test_queue_final_dlq"), anyBoolean(), anyBoolean(), anyBoolean(), isNull());

        // When / Then
        RabbitMQConnectionException thrown = assertThrows(RabbitMQConnectionException.class, () -> {
            rabbitMQService.connect(null);
        });

        assertTrue(thrown.getMessage().contains("Error estableciendo conexión o declarando colas de RabbitMQ"));
        assertTrue(thrown.getCause() instanceof IOException);

        // Verify interactions
        verify(mockChannel, times(1)).queueDeclarePassive(eq("test_queue"));
        verify(mockChannel, times(1)).queueDeclare(
                eq("test_queue_final_dlq"), anyBoolean(), anyBoolean(), anyBoolean(), isNull());
        verify(mockChannel, never()).queueDeclare(eq("email_retry_queue"), anyBoolean(), anyBoolean(), anyBoolean(), any());
        verify(mockChannel, never()).queueDeclare(eq("test_queue"), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }
}
