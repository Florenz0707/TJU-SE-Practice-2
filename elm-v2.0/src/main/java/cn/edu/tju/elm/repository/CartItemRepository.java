package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<Cart, Long> {
    List<Cart> findAllByBusinessIdAndCustomerId(Long businessId, Long customerId);
}
