package cn.edu.tju.food.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.edu.tju.FoodServiceApplication;
import cn.edu.tju.food.client.BusinessLookupClient;
import cn.edu.tju.food.model.bo.Food;
import cn.edu.tju.food.repository.FoodRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = FoodServiceApplication.class,
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:food-concurrency-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@ActiveProfiles("local")
class FoodInternalServiceConcurrencyIntegrationTest {

  @Autowired private FoodInternalService foodInternalService;
  @Autowired private FoodRepository foodRepository;

  @MockitoBean private BusinessLookupClient businessLookupClient;

  @AfterEach
  void tearDown() {
    foodRepository.deleteAll();
  }

  @Test
  void reserveStock_shouldAllowOnlyOneSuccess_whenConcurrentRequestsCompeteForLastItem()
      throws Exception {
    Food food = new Food();
    food.setFoodName("concurrent-food");
    food.setBusinessId(99L);
    food.setFoodPrice(new BigDecimal("18.00"));
    food.setStock(1);
    food.setDeleted(false);
    food = foodRepository.saveAndFlush(food);
    Long foodId = food.getId();

    when(businessLookupClient.exists(99L)).thenReturn(true);

    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Callable<Boolean> reserveTask =
          () -> {
            readyLatch.countDown();
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
              throw new IllegalStateException("start latch timeout");
            }
            long threadId = Thread.currentThread().threadId();
            return foodInternalService.reserveStock(
                "req-" + threadId,
                "order-" + threadId,
              List.of(new FoodInternalService.StockItemCommand(foodId, 1)));
          };

      Future<Boolean> first = executor.submit(reserveTask);
      Future<Boolean> second = executor.submit(reserveTask);

      assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
      startLatch.countDown();

      List<Boolean> results = new ArrayList<>();
      results.add(first.get(10, TimeUnit.SECONDS));
      results.add(second.get(10, TimeUnit.SECONDS));

      long successCount = results.stream().filter(Boolean.TRUE::equals).count();
      long failureCount = results.stream().filter(Boolean.FALSE::equals).count();

      assertThat(successCount).isEqualTo(1);
      assertThat(failureCount).isEqualTo(1);
      assertThat(foodRepository.findById(foodId)).isPresent();
      assertThat(foodRepository.findById(foodId).orElseThrow().getStock()).isZero();
    } finally {
      executor.shutdownNow();
    }
  }
}