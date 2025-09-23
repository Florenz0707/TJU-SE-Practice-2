package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BusinessApplication;
import cn.edu.tju.elm.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication,Long> {
    List<BusinessApplication> findALLByBusinessId(Long businessId);

}
