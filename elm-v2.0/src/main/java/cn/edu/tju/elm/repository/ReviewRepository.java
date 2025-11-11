package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.BO.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByCustomerId(Long userId);

    Optional<Review> findByOrderId(Long orderId);

    List<Review> findAllByBusinessId(Long businessId);
}
