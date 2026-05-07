package id.ac.ui.cs.advprog.bidmart.order.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisputeRequest {
    private String reason;
    private String description;
    private List<String> evidenceImages;
}
