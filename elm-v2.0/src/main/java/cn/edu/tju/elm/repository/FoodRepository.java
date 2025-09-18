package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Integer> {
}
