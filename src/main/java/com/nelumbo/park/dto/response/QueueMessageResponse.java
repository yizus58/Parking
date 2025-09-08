package com.nelumbo.park.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueMessageResponse {

    private String id;
    private String content;
    private String messageType;
    private int retryCount = 0;
    private int maxRetries = 3;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date failedAt;

    private boolean finalFailure = false;
    private String errorMessage;

    public QueueMessageResponse(String id, String content, String messageType) {
        this.id = id;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = new Date();
    }

    public boolean canRetry() {
        return retryCount < maxRetries && !finalFailure;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
