package cn.edu.tju.catalog.repository;

import cn.edu.tju.catalog.model.bo.Food;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {}
