package cn.edu.tju.merchant.repository;

import cn.edu.tju.merchant.model.Business;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {
  List<Business> findAllByBusinessOwnerId(Long businessOwnerId);
}
