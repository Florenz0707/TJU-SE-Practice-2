package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.MerchantApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, Long> {
  List<MerchantApplication> findAllByApplicantId(Long merchantId);
}
