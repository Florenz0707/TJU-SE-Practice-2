package cn.edu.tju.catalog.repository;

import cn.edu.tju.catalog.model.bo.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {}
