package id.ac.ui.cs.advprog.bidmart.order.dto;

import id.ac.ui.cs.advprog.bidmart.order.model.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
public class SaveNotification {
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> data;
}