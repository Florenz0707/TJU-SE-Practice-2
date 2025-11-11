package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final UserService userService;
    private final WalletService walletService;

    public WalletController(
            UserService userService,
            @Qualifier("walletServiceImpl") WalletService walletServiceImpl) {
        this.userService = userService;
        this.walletService = walletServiceImpl;
    }

    @GetMapping("/owner/{id}")
    public HttpResult<User> getWalletOwnerById(
            @PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

        return HttpResult.success(walletService.getWalletOwnerById(id));
    }

    @PostMapping("/voucher")
    public HttpResult<String> addVoucher(@RequestBody BigDecimal amount) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (amount == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NEGATIVE");

        WalletVO walletVO = walletService.addVoucher(amount, me);
        if (walletVO == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Something IS WRONG");
        return HttpResult.success("Add voucher successfully");
    }

    @GetMapping("")
    public HttpResult<WalletVO> getWalletByAuthorization() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        WalletVO walletVO = walletService.getWalletByOwner(me);
        if (walletVO == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
        return HttpResult.success(walletVO);
    }

    @PostMapping("")
    public HttpResult<WalletVO> createWalletByAuthorization() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        WalletVO walletVO = walletService.createWallet(me);
        if (walletVO == null)
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "YOU ALREADY HAVE A WALLET");
        return HttpResult.success(walletVO);
    }
}
