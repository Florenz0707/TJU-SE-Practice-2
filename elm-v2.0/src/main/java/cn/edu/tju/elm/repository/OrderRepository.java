package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findAllByCustomerId(Long id);

  List<Order> findAllByBusinessId(Long businessId);

  Order findByRequestId(String requestId);
}
