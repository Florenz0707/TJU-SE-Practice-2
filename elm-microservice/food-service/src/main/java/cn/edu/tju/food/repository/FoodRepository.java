package cn.edu.tju.food.repository;

import cn.edu.tju.food.model.bo.Food;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FoodRepository extends JpaRepository<Food, Long> {
  List<Food> findAllByBusinessId(Long businessId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from Food f where f.id = :id")
  Optional<Food> findByIdForUpdate(@Param("id") Long id);
}
