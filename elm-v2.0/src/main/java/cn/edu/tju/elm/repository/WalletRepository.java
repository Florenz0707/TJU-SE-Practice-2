package cn.edu.tju.elm.repository;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.model.BO.WalletBO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<WalletBO, Long> {
    Optional<WalletBO> findByOwnerId(Long ownerId);

    Optional<WalletBO> findByOwner(User owner);
}
