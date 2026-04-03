package cn.edu.tju.merchant.repository;

import cn.edu.tju.merchant.model.BusinessApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication, Long> {
  List<BusinessApplication> findAllByBusinessId(Long businessId);
}
