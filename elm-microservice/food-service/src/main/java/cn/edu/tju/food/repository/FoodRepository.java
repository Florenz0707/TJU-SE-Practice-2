package cn.edu.tju.food.repository;

import cn.edu.tju.food.model.bo.Food;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {
  List<Food> findAllByBusinessId(Long businessId);
}
