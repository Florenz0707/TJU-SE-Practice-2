package cn.edu.tju.points.repository;

import cn.edu.tju.points.model.BO.PointsAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
  Optional<PointsAccount> findByUserId(Long userId);
}
