package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.OrderDetailet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailetRepository extends JpaRepository<OrderDetailet, Long> {
  List<OrderDetailet> findAllByOrderId(Long orderId);
}
