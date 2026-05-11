package com.project.notificationservice.model;

import lombok.*;
import java.time.Instant;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationEvent {
    private String           userId;
    private String           email;
    private NotificationType type;
    private String           subject;
    private String           body;
    private Map<String, Object> metadata;
    private Instant          occurredAt;
}