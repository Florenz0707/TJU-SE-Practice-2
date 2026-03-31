package cn.edu.tju.elm.service.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.RECORD.TransactionsRecord;
import cn.edu.tju.elm.model.VO.TransactionVO;
import cn.edu.tju.elm.utils.InternalAccountClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

  @Mock private InternalAccountClient internalAccountClient;

  private TransactionServiceImpl transactionService;

  @BeforeEach
  public void setUp() {
    transactionService = new TransactionServiceImpl(internalAccountClient);
  }

  @Test
  public void testCreatePaymentAndFinish() throws Exception {
    BigDecimal amount = new BigDecimal("50.00");
    Long inWalletId = 10L;
    Long outWalletId = 20L;

    TransactionVO created = new TransactionVO();
    created.setId(1L);
    created.setAmount(amount);
    created.setType(TransactionType.PAYMENT);
    created.setInWalletId(inWalletId);
    created.setOutWalletId(outWalletId);
    created.setFinished(false);

    TransactionVO finished = new TransactionVO();
    finished.setId(1L);
    finished.setAmount(amount);
    finished.setType(TransactionType.PAYMENT);
    finished.setInWalletId(inWalletId);
    finished.setOutWalletId(outWalletId);
    finished.setFinished(true);

    when(internalAccountClient.createTransaction(amount, TransactionType.PAYMENT, inWalletId, outWalletId))
        .thenReturn(created);
    when(internalAccountClient.finishTransaction(1L, 999L, true)).thenReturn(finished);

    var txVo =
        transactionService.createTransaction(
            amount, TransactionType.PAYMENT, inWalletId, outWalletId);
    var finishedTx = transactionService.finishTransaction(1L, 999L, true);

    assertThat(txVo).isNotNull();
    assertThat(finishedTx.getFinished()).isTrue();

    verify(internalAccountClient).createTransaction(amount, TransactionType.PAYMENT, inWalletId, outWalletId);
    verify(internalAccountClient).finishTransaction(1L, 999L, true);
  }

  @Test
  public void testCreatePayment_insufficientBalance_throws() {
    Long inWalletId = 30L;
    Long outWalletId = 40L;

    BigDecimal amount = new BigDecimal("50.00");
    when(internalAccountClient.createTransaction(amount, TransactionType.PAYMENT, inWalletId, outWalletId))
      .thenThrow(new IllegalStateException(TransactionException.BALANCE_NOT_ENOUGH));

    assertThrows(
        TransactionException.class,
        () ->
            transactionService.createTransaction(
                amount, TransactionType.PAYMENT, inWalletId, outWalletId));
  }

  @Test
  public void getTransactionsByWalletId_delegatesToInternalClient() {
    TransactionVO inTx = new TransactionVO();
    inTx.setId(1L);
    TransactionVO outTx = new TransactionVO();
    outTx.setId(2L);
    TransactionsRecord record = new TransactionsRecord(List.of(inTx), List.of(outTx));

    when(internalAccountClient.getTransactionsByWalletId(eq(200L))).thenReturn(record);

    TransactionsRecord result = transactionService.getTransactionsByWalletId(200L);

    assertThat(result.inTransactions()).hasSize(1);
    assertThat(result.outTransactions()).hasSize(1);
    verify(internalAccountClient).getTransactionsByWalletId(200L);
  }
}
