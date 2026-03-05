package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private boolean read;
    private LocalDateTime createdAt;

}