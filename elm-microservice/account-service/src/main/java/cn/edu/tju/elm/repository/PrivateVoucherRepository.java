package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PrivateVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrivateVoucherRepository extends JpaRepository<PrivateVoucher, Long> {
  @Query(
      "SELECT pv FROM PrivateVoucher pv WHERE pv.wallet.ownerId = :ownerId AND (pv.deleted IS NULL"
          + " OR pv.deleted = false)")
  java.util.List<PrivateVoucher> findByWalletOwnerId(@Param("ownerId") Long ownerId);

  boolean existsByWalletIdAndPublicVoucherId(Long walletId, Long publicVoucherId);

  long countByPublicVoucherId(Long publicVoucherId);

  @Query(
      "SELECT COUNT(pv) FROM PrivateVoucher pv WHERE pv.publicVoucher.id = :publicVoucherId AND"
          + " pv.wallet.ownerId = :ownerId")
  long countByPublicVoucherIdAndWalletOwnerId(
      @Param("publicVoucherId") Long publicVoucherId, @Param("ownerId") Long ownerId);
}
