package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Integer> {
    List<Food> findAllByBusinessId(Long businessId);
}
