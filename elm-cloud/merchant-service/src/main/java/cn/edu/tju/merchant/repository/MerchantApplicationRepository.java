package cn.edu.tju.merchant.repository;

import cn.edu.tju.merchant.model.MerchantApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, Long> {
  List<MerchantApplication> findAllByApplicantId(Long merchantId);
}
