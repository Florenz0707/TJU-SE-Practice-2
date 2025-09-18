package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import cn.edu.tju.core.model.User;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
