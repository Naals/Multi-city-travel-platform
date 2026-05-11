package com.project.notificationservice.service;

import com.project.notificationservice.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void dispatch(NotificationEvent event) {
        log.info("📧 NOTIFICATION [{}] → userId={} subject='{}'",
                event.getType(),
                event.getUserId(),
                event.getSubject());
        log.info("   Body: {}", event.getBody());


    }
}