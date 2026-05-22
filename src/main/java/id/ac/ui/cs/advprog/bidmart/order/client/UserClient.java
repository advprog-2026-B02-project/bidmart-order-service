package id.ac.ui.cs.advprog.bidmart.order.client;

import id.ac.ui.cs.advprog.bidmart.order.dto.client.InternalUserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "user-service", url = "${app.client.user-url}")
public interface UserClient {
    @GetMapping("/internal/users/{userId}")
    InternalUserResponseDTO getUserById(@PathVariable("userId") UUID userId);
}