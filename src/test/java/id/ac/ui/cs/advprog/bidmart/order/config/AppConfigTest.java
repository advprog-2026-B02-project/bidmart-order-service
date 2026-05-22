package id.ac.ui.cs.advprog.bidmart.order.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppConfigTest {

    @Test
    void restTemplateBean_present() {
        AppConfig cfg = new AppConfig();
        assertNotNull(cfg.restTemplate());
    }
}
