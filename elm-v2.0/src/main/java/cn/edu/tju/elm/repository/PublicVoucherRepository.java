package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.PublicVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PublicVoucherRepository extends JpaRepository<PublicVoucher, Long> {
    List<PublicVoucher> findAllPublicVouchers();

    @Query("select v from PublicVoucher v where v.threshold >= :amount order by v.value desc")
    List<PublicVoucher> findQualifiedPublicVoucher(@Param("amount") BigDecimal amount);
}
