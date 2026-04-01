package cn.edu.tju.order.service;

import cn.edu.tju.order.model.bo.Review;
import cn.edu.tju.order.model.vo.ReviewSnapshotVO;
import cn.edu.tju.order.repository.ReviewRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewInternalService {
  private final ReviewRepository reviewRepository;

  public ReviewInternalService(ReviewRepository reviewRepository) {
    this.reviewRepository = reviewRepository;
  }

  @Transactional
  public ReviewSnapshotVO createReview(CreateReviewCommand command) {
    if (command == null
        || command.customerId() == null
        || command.businessId() == null
        || command.orderId() == null
        || command.anonymous() == null
        || command.stars() == null
        || command.content() == null) {
      throw new IllegalArgumentException("review required fields CANT BE NULL");
    }
    Review existing = reviewRepository.findByOrderIdAndDeletedFalse(command.orderId()).orElse(null);
    if (existing != null) {
      return new ReviewSnapshotVO(existing);
    }

    Review review = new Review();
    LocalDateTime now = LocalDateTime.now();
    review.setCreateTime(now);
    review.setUpdateTime(now);
    review.setDeleted(false);
    review.setCustomerId(command.customerId());
    review.setBusinessId(command.businessId());
    review.setOrderId(command.orderId());
    review.setAnonymous(command.anonymous());
    review.setStars(command.stars());
    review.setContent(command.content());
    Review saved = reviewRepository.save(review);
    return new ReviewSnapshotVO(saved);
  }

  @Transactional
  public ReviewSnapshotVO updateReview(Long reviewId, UpdateReviewCommand command) {
    if (reviewId == null || command == null) {
      throw new IllegalArgumentException("reviewId/review CANT BE NULL");
    }
    Review review =
        reviewRepository
            .findByIdAndDeletedFalse(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review NOT FOUND"));
    if (command.stars() != null) {
      review.setStars(command.stars());
    }
    if (command.content() != null) {
      review.setContent(command.content());
    }
    if (command.anonymous() != null) {
      review.setAnonymous(command.anonymous());
    }
    review.setUpdateTime(LocalDateTime.now());
    return new ReviewSnapshotVO(reviewRepository.save(review));
  }

  @Transactional(readOnly = true)
  public ReviewSnapshotVO getReviewById(Long reviewId) {
    if (reviewId == null) {
      return null;
    }
    return reviewRepository
        .findByIdAndDeletedFalse(reviewId)
        .map(ReviewSnapshotVO::new)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public ReviewSnapshotVO getReviewByOrderId(Long orderId) {
    if (orderId == null) {
      return null;
    }
    return reviewRepository
        .findByOrderIdAndDeletedFalse(orderId)
        .map(ReviewSnapshotVO::new)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<ReviewSnapshotVO> getReviewsByCustomerId(Long customerId) {
    if (customerId == null) {
      return List.of();
    }
    return reviewRepository.findAllByCustomerIdAndDeletedFalse(customerId).stream()
        .map(ReviewSnapshotVO::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReviewSnapshotVO> getReviewsByBusinessId(Long businessId) {
    if (businessId == null) {
      return List.of();
    }
    return reviewRepository.findAllByBusinessIdAndDeletedFalse(businessId).stream()
        .map(ReviewSnapshotVO::new)
        .toList();
  }

  @Transactional
  public ReviewSnapshotVO deleteReview(Long reviewId) {
    if (reviewId == null) {
      return null;
    }
    Review review =
        reviewRepository
            .findByIdAndDeletedFalse(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review NOT FOUND"));
    review.setDeleted(true);
    review.setUpdateTime(LocalDateTime.now());
    Review saved = reviewRepository.save(review);
    return new ReviewSnapshotVO(saved);
  }

  public record CreateReviewCommand(
      Long customerId,
      Long businessId,
      Long orderId,
      Boolean anonymous,
      Integer stars,
      String content) {}

  public record UpdateReviewCommand(Integer stars, String content, Boolean anonymous) {}
}
