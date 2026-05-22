package id.ac.ui.cs.advprog.bidmart.order.dto.client;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingDetailResponse {
    private UUID id;
    private UUID sellerId;
    private String title;
    private List<ListingImage> images;

    @Getter
    @Setter
    public static class ListingImage {
        private String url;
    }
}
