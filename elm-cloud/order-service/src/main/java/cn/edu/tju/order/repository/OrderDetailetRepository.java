package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.OrderDetailet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailetRepository extends JpaRepository<OrderDetailet, Long> {
  List<OrderDetailet> findAllByOrderId(Long orderId);
}
