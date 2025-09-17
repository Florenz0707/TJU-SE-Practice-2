package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
