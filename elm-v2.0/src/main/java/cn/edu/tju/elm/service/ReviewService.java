package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.Review;
import cn.edu.tju.elm.repository.ReviewRepository;
import cn.edu.tju.elm.utils.EntityUtils;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public ReviewService(
      ReviewRepository reviewRepository, ResponseCompatibilityEnricher compatibilityEnricher) {
    this.reviewRepository = reviewRepository;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  public void addReview(Review review) {
    reviewRepository.save(review);
  }

  public void updateReview(Review review) {
    reviewRepository.save(review);
  }

  public Review getReviewById(Long id) {
    Optional<Review> reviewOptional = reviewRepository.findById(id);
    Review review = reviewOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichReview(review);
    return review;
  }

  public Review getReviewByOrderId(Long orderId) {
    Optional<Review> reviewOptional = reviewRepository.findByOrderId(orderId);
    Review review = reviewOptional.map(EntityUtils::filterEntity).orElse(null);
    compatibilityEnricher.enrichReview(review);
    return review;
  }

  public List<Review> getReviewsByBusinessId(Long businessId) {
    List<Review> reviews =
        EntityUtils.filterEntityList(reviewRepository.findAllByBusinessId(businessId));
    compatibilityEnricher.enrichReviews(reviews);
    return reviews;
  }

  public List<Review> getReviewsByUserId(Long userId) {
    List<Review> reviews =
        EntityUtils.filterEntityList(reviewRepository.findAllByCustomerId(userId));
    compatibilityEnricher.enrichReviews(reviews);
    return reviews;
  }
}
