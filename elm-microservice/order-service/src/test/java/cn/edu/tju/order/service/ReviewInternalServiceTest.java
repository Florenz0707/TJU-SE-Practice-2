package cn.edu.tju.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.Review;
import cn.edu.tju.order.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewInternalServiceTest {
  @Mock private ReviewRepository reviewRepository;

  @InjectMocks private ReviewInternalService reviewInternalService;

  @Test
  void createReview_shouldReturnExistingWhenOrderAlreadyReviewed() {
    Review existing = new Review();
    existing.setId(1L);
    existing.setOrderId(10L);
    when(reviewRepository.findByOrderIdAndDeletedFalse(10L)).thenReturn(Optional.of(existing));

    var result =
        reviewInternalService.createReview(
            new ReviewInternalService.CreateReviewCommand(9L, 2L, 10L, false, 8, "great"));

    assertEquals(1L, result.getId());
  }

  @Test
  void getReviewsByBusinessId_shouldReturnList() {
    Review review = new Review();
    review.setId(1L);
    when(reviewRepository.findAllByBusinessIdAndDeletedFalse(2L)).thenReturn(List.of(review));

    var result = reviewInternalService.getReviewsByBusinessId(2L);

    assertEquals(1, result.size());
  }

  @Test
  void updateReview_shouldThrowWhenMissing() {
    when(reviewRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            reviewInternalService.updateReview(
                1L, new ReviewInternalService.UpdateReviewCommand(9, "ok", false)));
  }

  @Test
  void deleteReview_shouldMarkDeleted() {
    Review review = new Review();
    review.setId(1L);
    review.setDeleted(false);
    when(reviewRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(review));
    when(reviewRepository.save(review)).thenReturn(review);

    var result = reviewInternalService.deleteReview(1L);

    assertTrue(result.getId().equals(1L));
    assertTrue(review.getDeleted());
  }
}
