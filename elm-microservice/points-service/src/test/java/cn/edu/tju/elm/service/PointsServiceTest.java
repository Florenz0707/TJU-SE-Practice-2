package cn.edu.tju.elm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.elm.constant.PointsRecordType;
import cn.edu.tju.elm.exception.PointsException;
import cn.edu.tju.elm.model.BO.PointsAccount;
import cn.edu.tju.elm.model.BO.PointsBatch;
import cn.edu.tju.elm.model.BO.PointsRecord;
import cn.edu.tju.elm.repository.PointsAccountRepository;
import cn.edu.tju.elm.repository.PointsBatchRepository;
import cn.edu.tju.elm.repository.PointsRecordRepository;
import cn.edu.tju.elm.repository.PointsRuleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

  @Mock private PointsAccountRepository pointsAccountRepository;
  @Mock private PointsRuleRepository pointsRuleRepository;
  @Mock private PointsRecordRepository pointsRecordRepository;
  @Mock private PointsBatchRepository pointsBatchRepository;

  @InjectMocks private PointsService pointsService;

  @Test
  void getPointsAccountCreatesAccountWhenMissing() {
    when(pointsAccountRepository.findByUserId(1001L)).thenReturn(Optional.empty());
    when(pointsAccountRepository.save(any(PointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PointsAccount account = pointsService.getPointsAccount(1001L);

    assertThat(account.getUserId()).isEqualTo(1001L);
    assertThat(account.getTotalPoints()).isEqualTo(0);
    assertThat(account.getFrozenPoints()).isEqualTo(0);
    verify(pointsAccountRepository).save(any(PointsAccount.class));
  }

  @Test
  void freezePointsUpdatesAccountAndBatchesInFifoOrder() {
    PointsAccount account = PointsAccount.createNewAccount(2002L);
    account.setTotalPoints(120);

    PointsBatch firstBatch = createBatch(2002L, 40, LocalDateTime.now().plusDays(1));
    PointsBatch secondBatch = createBatch(2002L, 80, LocalDateTime.now().plusDays(2));

    when(pointsAccountRepository.findByUserId(2002L)).thenReturn(Optional.of(account));
    when(pointsBatchRepository.findAvailableBatchesByUserIdOrderByExpireTime(2002L))
        .thenReturn(List.of(firstBatch, secondBatch));
    when(pointsAccountRepository.save(any(PointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(pointsBatchRepository.save(any(PointsBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> result = pointsService.freezePoints(2002L, 70, "TMP-ORDER-1");

    assertThat(result).containsEntry("success", true).containsEntry("pointsUsed", 70);
    assertThat(account.getFrozenPoints()).isEqualTo(70);
    assertThat(account.getAvailablePoints()).isEqualTo(50);
    assertThat(firstBatch.getFrozenPoints()).isEqualTo(40);
    assertThat(firstBatch.getAvailablePoints()).isEqualTo(0);
    assertThat(firstBatch.getTempOrderId()).isEqualTo("TMP-ORDER-1");
    assertThat(secondBatch.getFrozenPoints()).isEqualTo(30);
    assertThat(secondBatch.getAvailablePoints()).isEqualTo(50);
    assertThat(secondBatch.getTempOrderId()).isEqualTo("TMP-ORDER-1");
  }

  @Test
  void refundDeductedPointsCreatesRefundRecordAndBatch() {
    PointsAccount account = PointsAccount.createNewAccount(3003L);
    account.setTotalPoints(20);

    PointsRecord consumeRecord =
        PointsRecord.createRecord(3003L, PointsRecordType.CONSUME, 30, "ORDER_9", "ORDER", "积分抵扣订单");

    when(pointsAccountRepository.findByUserId(3003L)).thenReturn(Optional.of(account));
    when(pointsRecordRepository.findTopByUserIdAndTypeAndBizIdOrderByRecordTimeDesc(
            3003L, PointsRecordType.CONSUME, "ORDER_9"))
        .thenReturn(Optional.of(consumeRecord));
    when(pointsRecordRepository.findTopByUserIdAndTypeAndBizIdOrderByRecordTimeDesc(
            3003L, PointsRecordType.EARN, "REFUND_ORDER_9"))
        .thenReturn(Optional.empty());
    when(pointsAccountRepository.save(any(PointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(pointsRecordRepository.save(any(PointsRecord.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(pointsBatchRepository.save(any(PointsBatch.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    boolean refunded = pointsService.refundDeductedPoints(3003L, "ORDER_9", "订单取消返还积分");

    assertThat(refunded).isTrue();
    assertThat(account.getTotalPoints()).isEqualTo(50);

    ArgumentCaptor<PointsRecord> recordCaptor = ArgumentCaptor.forClass(PointsRecord.class);
    verify(pointsRecordRepository).save(recordCaptor.capture());
    PointsRecord refundRecord = recordCaptor.getValue();
    assertThat(refundRecord.getType()).isEqualTo(PointsRecordType.EARN);
    assertThat(refundRecord.getBizId()).isEqualTo("REFUND_ORDER_9");
    assertThat(refundRecord.getPoints()).isEqualTo(30);

    ArgumentCaptor<PointsBatch> batchCaptor = ArgumentCaptor.forClass(PointsBatch.class);
    verify(pointsBatchRepository).save(batchCaptor.capture());
    PointsBatch refundBatch = batchCaptor.getValue();
    assertThat(refundBatch.getUserId()).isEqualTo(3003L);
    assertThat(refundBatch.getPoints()).isEqualTo(30);
    assertThat(refundBatch.getAvailablePoints()).isEqualTo(30);
    assertThat(refundBatch.getRecord()).isSameAs(refundRecord);
  }

  @Test
  void freezePointsThrowsWhenBalanceIsInsufficient() {
    PointsAccount account = PointsAccount.createNewAccount(4004L);
    account.setTotalPoints(20);

    when(pointsAccountRepository.findByUserId(4004L)).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> pointsService.freezePoints(4004L, 50, "TMP-ORDER-2"))
        .isInstanceOf(PointsException.class)
        .hasMessage(PointsException.INSUFFICIENT_POINTS);
  }

  private PointsBatch createBatch(Long userId, int points, LocalDateTime expireTime) {
    PointsBatch batch = new PointsBatch();
    batch.setUserId(userId);
    batch.setPoints(points);
    batch.setAvailablePoints(points);
    batch.setFrozenPoints(0);
    batch.setExpireTime(expireTime);
    return batch;
  }
}