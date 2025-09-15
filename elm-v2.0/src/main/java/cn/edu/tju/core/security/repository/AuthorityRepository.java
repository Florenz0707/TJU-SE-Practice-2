package cn.edu.tju.core.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import cn.edu.tju.core.model.Authority;

/**
 * Spring Data JPA repository for the {@link Authority} entity.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {
}
