package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<Cart, Long> {
}
