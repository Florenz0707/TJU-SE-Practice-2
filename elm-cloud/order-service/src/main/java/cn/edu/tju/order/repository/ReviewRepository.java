package cn.edu.tju.order.repository;

import cn.edu.tju.order.model.bo.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  Review findByOrderId(Long orderId);

  List<Review> findAllByCustomerId(Long customerId);

  List<Review> findAllByBusinessId(Long businessId);
}
