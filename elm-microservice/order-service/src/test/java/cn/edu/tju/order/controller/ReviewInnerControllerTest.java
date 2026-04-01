package cn.edu.tju.order.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.edu.tju.order.model.bo.Review;
import cn.edu.tju.order.model.vo.ReviewSnapshotVO;
import cn.edu.tju.order.service.ReviewInternalService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewInnerControllerTest {
  @Mock private ReviewInternalService reviewInternalService;

  @InjectMocks private ReviewInnerController reviewInnerController;

  @Test
  void createReview_shouldFailWhenMissingField() {
    ReviewInnerController.CreateReviewRequest request =
        new ReviewInnerController.CreateReviewRequest();
    var result = reviewInnerController.createReview(request);
    assertFalse(result.getSuccess());
  }

  @Test
  void getReviewsByCustomerId_shouldReturnData() {
    when(reviewInternalService.getReviewsByCustomerId(9L)).thenReturn(List.of());
    var result = reviewInnerController.getReviewsByCustomerId(9L);
    assertTrue(result.getSuccess());
  }

  @Test
  void updateReview_shouldFailWhenPayloadEmpty() {
    var result =
        reviewInnerController.updateReview(1L, new ReviewInnerController.UpdateReviewRequest());
    assertFalse(result.getSuccess());
  }

  @Test
  void getReviewById_shouldReturnData() {
    Review review = new Review();
    review.setId(1L);
    when(reviewInternalService.getReviewById(1L)).thenReturn(new ReviewSnapshotVO(review));
    var result = reviewInnerController.getReviewById(1L);
    assertTrue(result.getSuccess());
  }
}
