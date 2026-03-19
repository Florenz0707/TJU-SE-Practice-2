package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.IntegrationOutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationOutboxEventRepository
    extends JpaRepository<IntegrationOutboxEvent, Long> {
  @Query(
      "SELECT e FROM IntegrationOutboxEvent e WHERE e.status IN :statuses AND e.deleted = false"
          + " AND e.nextRetryAt <= :now ORDER BY e.createTime ASC")
  List<IntegrationOutboxEvent> findDispatchable(
      @Param("statuses") List<String> statuses, @Param("now") LocalDateTime now, Pageable pageable);
}
