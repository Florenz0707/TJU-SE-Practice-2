package cn.edu.tju.core.security.repository;

import cn.edu.tju.core.model.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<Authority, String> {}