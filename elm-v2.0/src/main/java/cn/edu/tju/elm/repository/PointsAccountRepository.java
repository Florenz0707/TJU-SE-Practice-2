package cn.edu.tju.elm.repository;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.BO.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserId(Long userId);

    Optional<PointsAccount> findByUser(User user);
}
