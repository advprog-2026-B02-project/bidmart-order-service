package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.order.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;

import java.util.UUID;

public interface NotificationService {

    NotificationListResponse getNotifications(UUID userId, Boolean isRead, int page, int size);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
    void saveNotification(SaveNotification dto);

}