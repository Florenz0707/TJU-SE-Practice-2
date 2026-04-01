package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Food;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {
  List<Food> findAllByBusinessId(Long businessId);
}
