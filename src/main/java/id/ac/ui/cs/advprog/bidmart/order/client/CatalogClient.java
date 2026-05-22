package id.ac.ui.cs.advprog.bidmart.order.client;

import id.ac.ui.cs.advprog.bidmart.order.dto.client.ListingDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "catalog-service", url = "${app.client.catalog-url}")
public interface CatalogClient {
    @GetMapping("/listings/{listingId}")
    ListingDetailResponse getListingById(@PathVariable("listingId") UUID listingId);
}
