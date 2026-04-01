package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Business;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {
  List<Business> findAllByBusinessOwnerId(Long businessOwnerId);
}
