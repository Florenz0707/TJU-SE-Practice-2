package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BusinessApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication,Long> {
    List<BusinessApplication> findAllByBusinessId(Long businessId);
}
