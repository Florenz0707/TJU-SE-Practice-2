package cn.edu.tju.food.repository;

import cn.edu.tju.food.model.bo.StockRequestLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRequestLogRepository extends JpaRepository<StockRequestLog, Long> {
  Optional<StockRequestLog> findByRequestId(String requestId);
}
