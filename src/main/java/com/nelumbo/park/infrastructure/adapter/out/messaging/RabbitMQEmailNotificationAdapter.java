package com.nelumbo.park.infrastructure.adapter.out.messaging;

import com.nelumbo.park.application.port.out.EmailNotificationPort;
import com.nelumbo.park.infrastructure.adapter.out.messaging.dto.EmailAttachmentResponse;
import com.nelumbo.park.infrastructure.adapter.out.messaging.dto.EmailDataResponse;
import com.nelumbo.park.infrastructure.adapter.out.messaging.dto.RabbitMQResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Queues emails for the mail service. The message shape (type plus a one item
 * data list with recipients, html, subject and attachments) is unchanged.
 */
@Component
public class RabbitMQEmailNotificationAdapter implements EmailNotificationPort {

    private final RabbitMQService rabbitMQService;
    private final String subject;
    private final String typeMessage;

    public RabbitMQEmailNotificationAdapter(RabbitMQService rabbitMQService,
                                            @Value("${app.subject}") String subject,
                                            @Value("${type.message}") String typeMessage) {
        this.rabbitMQService = rabbitMQService;
        this.subject = subject;
        this.typeMessage = typeMessage;
    }

    @Override
    public void sendEmail(String recipient, String html, List<EmailAttachment> attachments) {
        List<EmailAttachmentResponse> files = attachments.stream()
                .map(a -> new EmailAttachmentResponse(a.fileName(), a.storageKey()))
                .toList();
        EmailDataResponse data = new EmailDataResponse(recipient, html, subject, files);
        rabbitMQService.publishMessageBackoff(new RabbitMQResponse(typeMessage, data));
    }
}
