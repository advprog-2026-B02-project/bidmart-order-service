package id.ac.ui.cs.advprog.bidmart.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OrderListResponse {

    private List<OrderSummary> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

}