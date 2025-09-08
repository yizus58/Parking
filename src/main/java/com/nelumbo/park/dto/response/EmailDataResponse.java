package com.nelumbo.park.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDataResponse {

    private String id;
    private List<String> recipients;
    private String html;
    private String subject;
    private List<EmailAttachmentResponse> attachments;

    public EmailDataResponse(String id, String recipient, String html, String subject, List<EmailAttachmentResponse> attachments) {
        this.id = id;
        this.recipients = List.of(recipient);
        this.html = html;
        this.subject = subject;
        this.attachments = attachments;
    }

    public EmailDataResponse(String recipient, String html, String subject, List<EmailAttachmentResponse> attachments) {
        this.id = UUID.randomUUID().toString();
        this.recipients = List.of(recipient);
        this.html = html;
        this.subject = subject;
        this.attachments = attachments;
    }
}
