package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findAllByCustomerId(Long customerId);

  List<Order> findAllByBusinessId(Long businessId);

  Order findByRequestId(String requestId);
}
