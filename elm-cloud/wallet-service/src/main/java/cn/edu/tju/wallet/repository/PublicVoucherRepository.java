package cn.edu.tju.wallet.repository;

import cn.edu.tju.wallet.model.BO.PublicVoucher;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicVoucherRepository extends JpaRepository<PublicVoucher, Long> {
  @Query("select v from PublicVoucher v where v.threshold >= :amount order by v.faceValue desc")
  List<PublicVoucher> findQualifiedPublicVoucher(@Param("amount") BigDecimal amount);
}
