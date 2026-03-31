package cn.edu.tju.food.config;

import cn.edu.tju.food.client.BusinessLookupClient;
import cn.edu.tju.food.model.bo.Food;
import cn.edu.tju.food.repository.FoodRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FoodDataInitializer implements CommandLineRunner {

  private final FoodRepository foodRepository;
  private final BusinessLookupClient businessLookupClient;

  public FoodDataInitializer(FoodRepository foodRepository, BusinessLookupClient businessLookupClient) {
    this.foodRepository = foodRepository;
    this.businessLookupClient = businessLookupClient;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (foodRepository.count() > 0 || !businessLookupClient.exists(1L)) {
      return;
    }

    Food food = new Food();
    food.setFoodName("示例套餐");
    food.setFoodExplain("默认联调用菜品");
    food.setFoodPrice(new BigDecimal("25.00"));
    food.setBusinessId(1L);
    food.setStock(100);
    food.setRemarks("seed");
    food.setDeleted(false);
    food.setCreateTime(LocalDateTime.now());
    food.setUpdateTime(LocalDateTime.now());
    foodRepository.save(food);
  }
}