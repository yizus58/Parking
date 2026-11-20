package com.nelumbo.park.application.port.out;

import java.util.List;

public interface EmailNotificationPort {

    void sendEmail(String recipient, String html, List<EmailAttachment> attachments);

    /**
     * A file already uploaded to storage.
     *
     * @param fileName   name shown to the recipient
     * @param storageKey object key in storage
     */
    record EmailAttachment(String fileName, String storageKey) {
    }
}
