package cn.edu.tju.product.repository;

import cn.edu.tju.product.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByBusinessId(Long businessId);

    List<Food> findByBusinessIdIn(List<Long> businessIds);
}