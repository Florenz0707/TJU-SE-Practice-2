package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  Optional<Review> findByIdAndDeletedFalse(Long id);

  Optional<Review> findByOrderIdAndDeletedFalse(Long orderId);

  List<Review> findAllByCustomerIdAndDeletedFalse(Long customerId);

  List<Review> findAllByBusinessIdAndDeletedFalse(Long businessId);
}
