package id.ac.ui.cs.advprog.bidmart.order;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OrderServiceApplicationTest {

    @Test
    void main_runsWithoutStartingContext() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(OrderServiceApplication.class, new String[]{}))
                    .thenReturn(Mockito.mock(ConfigurableApplicationContext.class));

            assertDoesNotThrow(() -> OrderServiceApplication.main(new String[]{}));
        }
    }
}
