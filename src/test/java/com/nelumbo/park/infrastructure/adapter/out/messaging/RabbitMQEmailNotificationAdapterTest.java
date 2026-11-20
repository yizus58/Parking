package com.nelumbo.park.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.application.port.out.EmailNotificationPort.EmailAttachment;
import com.nelumbo.park.infrastructure.adapter.out.messaging.dto.RabbitMQResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMQEmailNotificationAdapterTest {

    @Test
    void publishesTheSameMessageShapeAsBefore() throws Exception {
        RabbitMQService rabbit = mock(RabbitMQService.class);
        RabbitMQEmailNotificationAdapter adapter = new RabbitMQEmailNotificationAdapter(rabbit, "Reporte diario", "email");

        adapter.sendEmail("socio@example.com", "<p>hola</p>", List.of(new EmailAttachment("reporte.xlsx", "key1")));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).publishMessageBackoff(captor.capture());
        JsonNode json = new ObjectMapper().valueToTree((RabbitMQResponse) captor.getValue());

        assertEquals("email", json.get("type").asText());
        assertEquals(1, json.get("data").size());
        JsonNode data = json.get("data").get(0);
        assertTrue(data.hasNonNull("id"));
        assertEquals("socio@example.com", data.get("recipients").get(0).asText());
        assertEquals("<p>hola</p>", data.get("html").asText());
        assertEquals("Reporte diario", data.get("subject").asText());
        assertEquals("reporte.xlsx", data.get("attachments").get(0).get("nameFile").asText());
        assertEquals("key1", data.get("attachments").get(0).get("s3Name").asText());
    }
}
