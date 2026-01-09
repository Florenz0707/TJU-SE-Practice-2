package cn.edu.tju.elm.service.serviceImpl;

import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.elm.model.BO.PrivateVoucher;
import cn.edu.tju.elm.model.BO.PublicVoucher;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.PublicVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
public class PrivateVoucherClaimIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PublicVoucherRepository publicVoucherRepository;

    @Autowired
    private PrivateVoucherRepository privateVoucherRepository;

    @Autowired
    private PrivateVoucherServiceImpl privateVoucherService;

    @Test
    @Transactional
    public void testDuplicateClaimIsPrevented() {
        // create user
        User user = new User();
        user.setUsername("dup_claim_user");
        user.setPassword("pwd");
        user.setActivated(true);
        user = userRepository.save(user);

        // create wallet
        Wallet wallet = Wallet.getNewWallet(user);
        wallet = walletRepository.save(wallet);

        // create public voucher
        PublicVoucher pv = PublicVoucher.createVoucher(BigDecimal.ZERO, new BigDecimal("10"), true, 30);
        pv = publicVoucherRepository.save(pv);

        PublicVoucherVO vo = new PublicVoucherVO(pv);

        // first claim should succeed
        boolean first = privateVoucherService.createPrivateVoucher(wallet.getId(), vo);
        Assertions.assertTrue(first, "first claim should succeed");

        // second claim should be prevented
        boolean second = privateVoucherService.createPrivateVoucher(wallet.getId(), vo);
        Assertions.assertFalse(second, "second claim should be prevented by uniqueness check");

        // DB should contain only one private voucher for this wallet-publicVoucher pair
        long count = privateVoucherRepository.findAll().stream()
                .filter(p -> p.getWallet().getId().equals(wallet.getId()) && p.getPublicVoucher() != null && p.getPublicVoucher().getId().equals(pv.getId()))
                .count();
        Assertions.assertEquals(1L, count, "only one private voucher record expected");
    }
}
