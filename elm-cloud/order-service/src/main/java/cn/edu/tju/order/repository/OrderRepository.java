package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.Order;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findAllByCustomerId(Long customerId);

  List<Order> findAllByBusinessId(Long businessId);

  @Query("SELECT o FROM Order o WHERE o.businessId IN :businessIds")
  List<Order> findAllByBusinessIdIn(@Param("businessIds") List<Long> businessIds);

  Page<Order> findAllByCustomerId(Long customerId, Pageable pageable);

  Page<Order> findAllByBusinessId(Long businessId, Pageable pageable);

  @Query("SELECT o FROM Order o WHERE o.businessId IN :businessIds")
  Page<Order> findAllByBusinessIdIn(@Param("businessIds") List<Long> businessIds, Pageable pageable);

  Order findByRequestId(String requestId);
}
