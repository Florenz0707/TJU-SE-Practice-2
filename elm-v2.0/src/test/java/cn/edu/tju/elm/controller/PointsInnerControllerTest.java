package cn.edu.tju.elm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsInnerControllerTest {

  @Mock private PointsService pointsService;

  @InjectMocks private PointsInnerController pointsInnerController;

  @Test
  void refundDeductedPoints_shouldReturnSuccessWhenServiceReturnsTrue() {
    PointsInnerController.RefundDeductedPointsRequest request =
        new PointsInnerController.RefundDeductedPointsRequest();
    request.setUserId(1001);
    request.setOrderBizId("ORDER_123");
    request.setReason("订单取消返还积分");

    when(pointsService.refundDeductedPoints(1001L, "ORDER_123", "订单取消返还积分")).thenReturn(true);

    HttpResult<Boolean> result = pointsInnerController.refundDeductedPoints(request);

    assertTrue(result.getSuccess());
    assertEquals(Boolean.TRUE, result.getData());
    verify(pointsService).refundDeductedPoints(1001L, "ORDER_123", "订单取消返还积分");
  }

  @Test
  void notifyReviewDeleted_shouldReturnSuccessWhenServiceReturnsTrue() {
    PointsInnerController.ReviewDeletedRequest request =
        new PointsInnerController.ReviewDeletedRequest();
    request.setUserId(2002);
    request.setReviewId("REVIEW_888");

    when(pointsService.notifyReviewDeleted(2002L, "REVIEW_888")).thenReturn(true);

    HttpResult<Boolean> result = pointsInnerController.notifyReviewDeleted(request);

    assertTrue(result.getSuccess());
    assertEquals(Boolean.TRUE, result.getData());
    verify(pointsService).notifyReviewDeleted(2002L, "REVIEW_888");
  }

  @Test
  void notifyReviewDeleted_shouldReturnFailureWhenPointsExceptionThrown() {
    PointsInnerController.ReviewDeletedRequest request =
        new PointsInnerController.ReviewDeletedRequest();
    request.setUserId(2002);
    request.setReviewId("REVIEW_404");

    when(pointsService.notifyReviewDeleted(2002L, "REVIEW_404"))
        .thenThrow(new PointsException(PointsException.ACCOUNT_NOT_FOUND));

    HttpResult<Boolean> result = pointsInnerController.notifyReviewDeleted(request);

    assertFalse(result.getSuccess());
    assertEquals(ResultCodeEnum.SERVER_ERROR.getCode(), result.getCode());
    assertEquals(PointsException.ACCOUNT_NOT_FOUND, result.getMessage());
  }
}
