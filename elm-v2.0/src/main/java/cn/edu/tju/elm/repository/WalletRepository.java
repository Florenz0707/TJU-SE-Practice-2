package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
  Optional<Wallet> findByOwnerId(Long ownerId);
}
