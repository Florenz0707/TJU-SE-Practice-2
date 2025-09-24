package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.MerchantApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication,Long> {
    List<MerchantApplication> findAllByApplicantId(Long merchantId);

}
