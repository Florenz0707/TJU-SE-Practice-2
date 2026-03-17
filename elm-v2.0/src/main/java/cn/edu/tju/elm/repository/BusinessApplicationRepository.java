package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.BusinessApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication, Long> {
  List<BusinessApplication> findAllByBusinessId(Long businessId);
}
