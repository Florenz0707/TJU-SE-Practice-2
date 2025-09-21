package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findAllByBusinessId(Long businessId);
}
