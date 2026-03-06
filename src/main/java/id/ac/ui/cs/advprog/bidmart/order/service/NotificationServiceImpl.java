package id.ac.ui.cs.advprog.bidmart.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.order.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmart.order.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.order.model.Notification;
import id.ac.ui.cs.advprog.bidmart.order.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId, Boolean isRead, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> result;

        if (isRead != null) {
            result = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        } else {
            result = notificationRepository.findByUserId(userId, pageable);
        }

        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);

        List<NotificationResponse> content = result.getContent()
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return NotificationListResponse.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notifikasi tidak ditemukan"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak memiliki akses ke notifikasi ini");
        }

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional
    public void saveNotification(SaveNotification dto) {
        Notification notification = new Notification();
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());

        if (dto.getData() != null) {
            try {
                notification.setData(objectMapper.writeValueAsString(dto.getData()));
            } catch (JsonProcessingException e) {
                notification.setData(null);
            }
        }

        notificationRepository.save(notification);
    }

    private NotificationResponse toResponseDTO(Notification notification) {
        Map<String, Object> dataMap = null;
        if (notification.getData() != null) {
            try {
                dataMap = objectMapper.readValue(notification.getData(), Map.class);
            } catch (JsonProcessingException e) {
                dataMap = null;
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(dataMap)
                .read(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}