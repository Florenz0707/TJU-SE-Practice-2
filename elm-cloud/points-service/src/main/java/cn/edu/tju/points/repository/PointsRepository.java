package cn.edu.tju.points.repository;

import cn.edu.tju.points.model.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointsRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserId(Long userId);
}
