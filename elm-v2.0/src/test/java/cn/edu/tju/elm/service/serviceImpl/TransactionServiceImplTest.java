package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.elm.constant.TransactionType;
import cn.edu.tju.elm.exception.TransactionException;
import cn.edu.tju.elm.model.BO.Transaction;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.repository.TransactionRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PublicVoucherService publicVoucherService;

    @Mock
    private PrivateVoucherService privateVoucherService;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    public void setUp() {
        transactionService = new TransactionServiceImpl(walletRepository, transactionRepository, publicVoucherService, privateVoucherService);
    }

    @Test
    public void testCreatePaymentAndFinish() throws Exception {
        // 准备 payer (outWallet) 和 receiver (inWallet)
        User payerUser = new User();
        payerUser.setUsername("payer");
        Wallet outWallet = Wallet.getNewWallet(payerUser);
        // 反射或直接设置初始余额（Wallet 默认值为 0），此处通过 addBalance 增加
        outWallet.addBalance(new BigDecimal("100.00"));

        User receiverUser = new User();
        receiverUser.setUsername("receiver");
        Wallet inWallet = Wallet.getNewWallet(receiverUser);
        inWallet.addBalance(new BigDecimal("10.00"));

        Long inWalletId = 10L;
        Long outWalletId = 20L;

        // 模拟 repository 返回
        when(walletRepository.findById(eq(inWalletId))).thenReturn(Optional.of(inWallet));
        when(walletRepository.findById(eq(outWalletId))).thenReturn(Optional.of(outWallet));

        // capture saved transaction
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(txCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // 创建 payment 交易
        BigDecimal amount = new BigDecimal("50.00");
        var txVo = transactionService.createTransaction(amount, TransactionType.PAYMENT, inWalletId, outWalletId);

        // 验证付款方余额减少
        assertThat(outWallet.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));

        // 模拟根据 id 查找 transaction（这里用任意 id，返回捕获到的 transaction 对象）
        Transaction saved = txCaptor.getValue();
        // 假设 id 为 1
        saved.setId(1L);
        when(transactionRepository.findById(eq(1L))).thenReturn(Optional.of(saved));

        // 完成交易（入账给收款方）
        User operator = new User();
        operator.setUsername("op");
        var finished = transactionService.finishTransaction(1L, operator);

        // 收款方余额增加
        assertThat(inWallet.getBalance()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(finished.getFinished()).isTrue();

        verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    public void testCreatePayment_insufficientBalance_throws() {
        User payerUser = new User();
        payerUser.setUsername("payer");
        Wallet outWallet = Wallet.getNewWallet(payerUser);
        outWallet.addBalance(new BigDecimal("10.00"));

        User receiverUser = new User();
        receiverUser.setUsername("receiver");
        Wallet inWallet = Wallet.getNewWallet(receiverUser);

        Long inWalletId = 30L;
        Long outWalletId = 40L;

        when(walletRepository.findById(eq(inWalletId))).thenReturn(Optional.of(inWallet));
        when(walletRepository.findById(eq(outWalletId))).thenReturn(Optional.of(outWallet));

        BigDecimal amount = new BigDecimal("50.00");
        assertThrows(TransactionException.class, () -> transactionService.createTransaction(amount, TransactionType.PAYMENT, inWalletId, outWalletId));
    }

    @Test
    public void concurrencySimulation_multiplePayments_reduceBalanceAtomically() throws Exception {
        // 该测试为示例，模拟多线程环境下并发调用 createTransaction
        User payerUser = new User();
        payerUser.setUsername("payer");
        Wallet outWallet = Wallet.getNewWallet(payerUser);
        outWallet.addBalance(new BigDecimal("1000.00"));

        User receiverUser = new User();
        receiverUser.setUsername("receiver");
        Wallet inWallet = Wallet.getNewWallet(receiverUser);

        Long inWalletId = 100L;
        Long outWalletId = 200L;

        // 对 findById 的并发访问返回同一实例（注意：真实环境应使用 DB 锁）
        when(walletRepository.findById(eq(inWalletId))).thenReturn(Optional.of(inWallet));
        when(walletRepository.findById(eq(outWalletId))).thenReturn(Optional.of(outWallet));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int threads = 10;
        BigDecimal each = new BigDecimal("50.00");
        ExecutorService es = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    start.await();
                    try {
                        transactionService.createTransaction(each, TransactionType.PAYMENT, inWalletId, outWalletId);
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        // 开始并发
        start.countDown();
        done.await();
        es.shutdown();

        // 期望总扣款 = threads * each
        BigDecimal expected = new BigDecimal("1000.00").subtract(each.multiply(new BigDecimal(threads)));
        assertThat(outWallet.getBalance()).isEqualByComparingTo(expected);
        // failures 表示并发过程中的异常数量（理论上应为0），考虑到非事务性并发风险，此测试更多用于说明并发影响
    }
}
