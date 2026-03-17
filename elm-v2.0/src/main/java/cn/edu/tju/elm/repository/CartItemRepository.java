package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Cart;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<Cart, Long> {
  List<Cart> findAllByBusinessIdAndCustomerId(Long businessId, Long customerId);

  List<Cart> findAllByCustomerId(Long customerId);
}
