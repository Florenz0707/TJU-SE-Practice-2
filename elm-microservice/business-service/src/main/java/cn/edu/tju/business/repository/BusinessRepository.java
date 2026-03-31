package cn.edu.tju.business.repository;

import cn.edu.tju.business.model.bo.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {}
