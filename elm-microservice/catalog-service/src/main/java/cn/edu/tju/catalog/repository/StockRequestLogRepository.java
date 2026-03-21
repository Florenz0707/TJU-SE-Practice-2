package cn.edu.tju.catalog.repository;

import cn.edu.tju.catalog.model.bo.StockRequestLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRequestLogRepository extends JpaRepository<StockRequestLog, Long> {
  Optional<StockRequestLog> findByRequestId(String requestId);
}
