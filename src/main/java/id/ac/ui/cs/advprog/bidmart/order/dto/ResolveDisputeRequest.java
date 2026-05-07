package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolveDisputeRequest {
    private String resolution;
    private String note;
}
