package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.Cart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
  Optional<Cart> findByIdAndDeletedFalse(Long id);

  List<Cart> findAllByCustomerIdAndDeletedFalse(Long customerId);

  List<Cart> findAllByBusinessIdAndCustomerIdAndDeletedFalse(Long businessId, Long customerId);
}
