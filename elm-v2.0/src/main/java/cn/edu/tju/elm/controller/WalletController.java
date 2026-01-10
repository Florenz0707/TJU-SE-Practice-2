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

        try {
            User owner = walletService.getWalletOwnerById(id);
            return HttpResult.success(owner);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{id}")
    public HttpResult<WalletVO> getWalletById(
            @PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

        try {
            WalletVO walletVO = walletService.getWalletById(id, me);
            return HttpResult.success(walletVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/voucher/{walletId}")
    public HttpResult<String> addVoucher(
            @PathVariable Long walletId,
            @RequestBody BigDecimal amount) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

        if (amount == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NEGATIVE");

        try {
            walletService.addVoucher(walletId, amount);
            return HttpResult.success("Add voucher successfully");
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/my")
    public HttpResult<WalletVO> getWalletByAuthorization() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        try {
            WalletVO walletVO = walletService.getWalletByOwner(me);
            return HttpResult.success(walletVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("")
    public HttpResult<WalletVO> createWalletByAuthorization() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        try {
            WalletVO walletVO = walletService.createWallet(me);
            return HttpResult.success(walletVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }
}
