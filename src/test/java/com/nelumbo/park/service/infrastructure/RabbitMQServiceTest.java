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
    @Spy
    private RabbitMQService rabbitMQService;

    @Mock
    private Connection mockConnection;
    @Mock
    private Channel mockChannel;
    @Mock
    private ObjectMapper mockObjectMapper;
    @Mock
    private ConnectionFactory mockConnectionFactory;

    @BeforeEach
    void setUp() throws IOException, TimeoutException {
        ReflectionTestUtils.setField(rabbitMQService, "rabbitUrl", "amqp://localhost");
        ReflectionTestUtils.setField(rabbitMQService, "defaultQueueName", "test_queue");
        ReflectionTestUtils.setField(rabbitMQService, "defaultFinalQueueName", "_final_dlq");

        doReturn(mockConnectionFactory).when(rabbitMQService).createConnectionFactory();

        when(mockConnectionFactory.newConnection()).thenReturn(mockConnection);
        when(mockConnection.createChannel()).thenReturn(mockChannel);

        when(mockConnection.isOpen()).thenReturn(true);
        when(mockChannel.isOpen()).thenReturn(true);
        when(mockObjectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
    }

    @Test
    @DisplayName("Should send message to queue successfully")
    void sendToQueue_Success() throws IOException {

        String queueName = "test_queue";
        Object message = new Object();

        rabbitMQService.sendToQueue(queueName, message);

        verify(mockObjectMapper, times(1)).writeValueAsBytes(message);
        verify(mockChannel, times(1)).basicPublish(
                eq(""), eq(queueName), any(AMQP.BasicProperties.class), eq("{}".getBytes()));
    }

    @Test
    @DisplayName("Should connect and send message if channel is null")
    void sendToQueue_ChannelNull_ConnectsAndSends() throws IOException, TimeoutException {

        ReflectionTestUtils.setField(rabbitMQService, "channel", null);
        String queueName = "test_queue";
        Object message = new Object();

        rabbitMQService.sendToQueue(queueName, message);

        verify(rabbitMQService, times(1)).connect(null);
        verify(mockObjectMapper, times(1)).writeValueAsBytes(message);
        verify(mockChannel, times(1)).basicPublish(
                eq(""), eq(queueName), any(AMQP.BasicProperties.class), eq("{}".getBytes()));
    }

    @Test
    @DisplayName("Should throw RabbitMQMessagePublishException on IOException during publish")
    void sendToQueue_IOException_ThrowsException() throws IOException {

        String queueName = "test_queue";
        Object message = new Object();
        doThrow(new IOException("Test IO Exception")).when(mockChannel).basicPublish(
                anyString(), anyString(), any(AMQP.BasicProperties.class), any(byte[].class));

        RabbitMQMessagePublishException thrown = assertThrows(RabbitMQMessagePublishException.class, () -> {
            rabbitMQService.sendToQueue(queueName, message);
        });

        assertTrue(thrown.getMessage().contains("Error publicando mensaje en la cola: test_queue"));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Should throw RabbitMQMessagePublishException if all backoff attempts fail")
    void publishMessageBackoff_AllAttemptsFail_ThrowsException() throws IOException {

        Object message = new Object();

        doThrow(new IOException("Publishing failed consistently"))
                .when(mockChannel).basicPublish(anyString(), anyString(), any(), any());

        RabbitMQMessagePublishException thrown = assertThrows(RabbitMQMessagePublishException.class, () -> {
            rabbitMQService.publishMessageBackoff(message);
        });

        assertTrue(thrown.getMessage().contains("Fallo final al publicar mensaje con backoff"));

        verify(rabbitMQService, atLeast(6)).connect(null);
        verify(mockChannel, atLeast(6)).basicPublish(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("destroy should close channel and connection")
    void destroy_ClosesChannelAndConnection() throws IOException, TimeoutException {

        ReflectionTestUtils.setField(rabbitMQService, "channel", mockChannel);
        ReflectionTestUtils.setField(rabbitMQService, "connection", mockConnection);

        rabbitMQService.destroy();

        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @DisplayName("destroy should handle IOException when closing channel")
    void destroy_HandlesIOException_Channel() throws IOException, TimeoutException {
        ReflectionTestUtils.setField(rabbitMQService, "channel", mockChannel);
        ReflectionTestUtils.setField(rabbitMQService, "connection", mockConnection);
        doThrow(new IOException("Channel close error")).when(mockChannel).close();

        rabbitMQService.destroy();

        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @DisplayName("destroy should handle IOException when closing connection")
    void destroy_HandlesIOException_Connection() throws IOException, TimeoutException {
        ReflectionTestUtils.setField(rabbitMQService, "channel", mockChannel);
        ReflectionTestUtils.setField(rabbitMQService, "connection", mockConnection);
        doThrow(new IOException("Connection close error")).when(mockConnection).close();

        rabbitMQService.destroy();

        verify(mockChannel, times(1)).close();
        verify(mockConnection, times(1)).close();
    }

    @Test
    @DisplayName("publishToDeadLetterQueue should call sendToQueue with retry queue name")
    void publishToDeadLetterQueue_CallsSendToQueue() {
        QueueMessageResponse message = new QueueMessageResponse();
        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());

        rabbitMQService.publishToDeadLetterQueue(message);

        verify(rabbitMQService, times(1)).sendToQueue("email_retry_queue", message);
    }

    @Test
    @DisplayName("publishToFinalDLQ should call sendToQueue with final DLQ name and update message")
    void publishToFinalDLQ_CallsSendToQueueAndUpdateMessage() {
        QueueMessageResponse message = new QueueMessageResponse();
        message.setFailedAt(null);
        message.setFinalFailure(false);

        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());

        rabbitMQService.publishToFinalDLQ(message);

        verify(rabbitMQService, times(1)).sendToQueue("test_queue_final_dlq", message);
        assertNotNull(message.getFailedAt());
        assertTrue(message.isFinalFailure());
    }

    @Test
    @DisplayName("consumeFromQueue should set up consumer and process message successfully")
    void consumeFromQueue_Success() throws IOException {

        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);
        QueueMessageResponse testMessage = new QueueMessageResponse();

        when(mockObjectMapper.readValue(any(byte[].class), eq(QueueMessageResponse.class)))
                .thenReturn(testMessage);

        ArgumentCaptor<com.rabbitmq.client.DeliverCallback> deliverCallbackCaptor =
                ArgumentCaptor.forClass(com.rabbitmq.client.DeliverCallback.class);

        rabbitMQService.consumeFromQueue(queueName, mockCallback);

        verify(mockChannel, times(1)).basicConsume(
                eq(queueName), eq(false), deliverCallbackCaptor.capture(), any(com.rabbitmq.client.CancelCallback.class));

        com.rabbitmq.client.DeliverCallback capturedCallback = deliverCallbackCaptor.getValue();
        com.rabbitmq.client.Envelope mockEnvelope = mock(com.rabbitmq.client.Envelope.class);
        when(mockEnvelope.getDeliveryTag()).thenReturn(123L);
        com.rabbitmq.client.Delivery mockDelivery = new com.rabbitmq.client.Delivery(
                mockEnvelope, mock(AMQP.BasicProperties.class), "{}".getBytes());

        capturedCallback.handle("consumerTag", mockDelivery);

        verify(mockCallback, times(1)).accept(testMessage);
        verify(mockChannel, times(1)).basicAck(123L, false);
        verify(mockChannel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("consumeFromQueue should handle IOException during message processing and nack message")
    void consumeFromQueue_IOException_NacksMessage() throws IOException {
        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);

        when(mockObjectMapper.readValue(any(byte[].class), eq(QueueMessageResponse.class)))
                .thenThrow(new IOException("Error parsing message"));

        ArgumentCaptor<com.rabbitmq.client.DeliverCallback> deliverCallbackCaptor =
                ArgumentCaptor.forClass(com.rabbitmq.client.DeliverCallback.class);

        rabbitMQService.consumeFromQueue(queueName, mockCallback);

        verify(mockChannel, times(1)).basicConsume(
                eq(queueName), eq(false), deliverCallbackCaptor.capture(), any(com.rabbitmq.client.CancelCallback.class));

        com.rabbitmq.client.DeliverCallback capturedCallback = deliverCallbackCaptor.getValue();
        com.rabbitmq.client.Envelope mockEnvelope = mock(com.rabbitmq.client.Envelope.class);
        when(mockEnvelope.getDeliveryTag()).thenReturn(456L);
        com.rabbitmq.client.Delivery mockDelivery = new com.rabbitmq.client.Delivery(
                mockEnvelope, mock(AMQP.BasicProperties.class), "invalid_bytes".getBytes());

        capturedCallback.handle("consumerTag", mockDelivery);

        verify(mockCallback, never()).accept(any());
        verify(mockChannel, never()).basicAck(anyLong(), anyBoolean());
        verify(mockChannel, times(1)).basicNack(456L, false, false);
    }

    @Test
    @DisplayName("consumeFromQueue should throw RabbitMQConsumerException on IOException during basicConsume setup")
    void consumeFromQueue_BasicConsumeIOException_ThrowsException() throws IOException {

        String queueName = "test_consumer_queue";
        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);
        doThrow(new IOException("Basic consume setup error")).when(mockChannel).basicConsume(
                anyString(), anyBoolean(), any(com.rabbitmq.client.DeliverCallback.class), any(com.rabbitmq.client.CancelCallback.class));

        RabbitMQConsumerException thrown = assertThrows(RabbitMQConsumerException.class, () -> {
            rabbitMQService.consumeFromQueue(queueName, mockCallback);
        });

        assertTrue(thrown.getMessage().contains("Error configurando consumidor para la cola: test_consumer_queue"));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("consumeFromFinalDLQ should call consumeFromQueue with final DLQ name")
    void consumeFromFinalDLQ_CallsConsumeFromQueue() {

        Consumer<QueueMessageResponse> mockCallback = mock(Consumer.class);

        doNothing().when(rabbitMQService).sendToQueue(anyString(), any());


        rabbitMQService.consumeFromFinalDLQ(mockCallback);


        verify(rabbitMQService, times(1)).consumeFromQueue("test_queue_final_dlq", mockCallback);
    }

    @Test
    @DisplayName("connect should establish connection and declare queues if not passive")
    void connect_EstablishesConnectionAndDeclaresQueues() throws Exception {

        doThrow(new IOException("Queue not found")).when(mockChannel).queueDeclarePassive(anyString());

        rabbitMQService.connect(null);

        verify(mockChannel, times(1)).queueDeclarePassive("test_queue");

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

        when(mockChannel.queueDeclarePassive(anyString())).thenReturn(mock(AMQP.Queue.DeclareOk.class));

        rabbitMQService.connect(null);

        verify(mockChannel, times(1)).queueDeclarePassive("test_queue");

        verify(mockChannel, never()).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @DisplayName("connect should throw RabbitMQConnectionException if newConnection fails")
    void connect_NewConnectionFails_ThrowsException() throws IOException, TimeoutException {

        doThrow(new IOException("Connection failed")).when(mockConnectionFactory).newConnection();

        RabbitMQConnectionException thrown = assertThrows(RabbitMQConnectionException.class, () -> {
            rabbitMQService.connect(null);
        });

        assertTrue(thrown.getMessage().contains("Error estableciendo conexión o declarando colas de RabbitMQ"));
        assertTrue(thrown.getCause() instanceof IOException);

        verify(mockConnectionFactory, times(1)).newConnection();

        verify(mockChannel, never()).queueDeclarePassive(anyString());
        verify(mockChannel, never()).queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @DisplayName("connect should throw RabbitMQConnectionException if queue declaration fails")
    void connect_QueueDeclarationFails_ThrowsRabbitMQConnectionException() throws IOException, TimeoutException {

        doThrow(new IOException("Passive declare failed")).when(mockChannel).queueDeclarePassive(anyString());

        doThrow(new IOException("Final DLQ declare failed")).when(mockChannel).queueDeclare(
                eq("test_queue_final_dlq"), anyBoolean(), anyBoolean(), anyBoolean(), isNull());

        RabbitMQConnectionException thrown = assertThrows(RabbitMQConnectionException.class, () -> {
            rabbitMQService.connect(null);
        });

        assertTrue(thrown.getMessage().contains("Error estableciendo conexión o declarando colas de RabbitMQ"));
        assertTrue(thrown.getCause() instanceof IOException);

        verify(mockChannel, times(1)).queueDeclarePassive("test_queue");
        verify(mockChannel, times(1)).queueDeclare(
                eq("test_queue_final_dlq"), anyBoolean(), anyBoolean(), anyBoolean(), isNull());
        verify(mockChannel, never()).queueDeclare(eq("email_retry_queue"), anyBoolean(), anyBoolean(), anyBoolean(), any());
        verify(mockChannel, never()).queueDeclare(eq("test_queue"), anyBoolean(), anyBoolean(), anyBoolean(), any());
    }
}
