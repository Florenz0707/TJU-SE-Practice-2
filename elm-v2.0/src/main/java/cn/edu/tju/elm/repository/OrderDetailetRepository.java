package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.OrderDetailet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailetRepository extends JpaRepository<OrderDetailet, Long> {
    List<OrderDetailet> findAllByOrderId(Long orderId);
}
