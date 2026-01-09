package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.exception.PrivateVoucherException;
import cn.edu.tju.elm.model.VO.PrivateVoucherVO;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.PrivateVoucherService;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/privateVoucher")
public class PrivateVoucherController {
    private final UserService userService;
    private final PublicVoucherService publicVoucherService;
    private final PrivateVoucherService privateVoucherService;
    private final WalletService walletService;

    public PrivateVoucherController(UserService userService,
                                    PublicVoucherService publicVoucherService,
                                    PrivateVoucherService privateVoucherService,
                                    WalletService walletService) {
        this.userService = userService;
        this.publicVoucherService = publicVoucherService;
        this.privateVoucherService = privateVoucherService;
        this.walletService = walletService;
    }

    @PostMapping("/claim/{publicVoucherId}")
    public HttpResult<String> claimPublicVoucher(@PathVariable Long publicVoucherId) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        User me = meOptional.get();
        try {
            PublicVoucherVO publicVoucherVO = publicVoucherService.getPublicVoucherById(publicVoucherId);
            if (publicVoucherVO == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "PublicVoucher NOT FOUND");
            WalletVO walletVO = walletService.getWalletByOwner(me);
            if (walletVO == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Wallet NOT FOUND");
            boolean ok = privateVoucherService.createPrivateVoucher(walletVO.getId(), publicVoucherVO);
            if (ok) return HttpResult.success("Claimed");
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Claim Failed");
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/my")
    public HttpResult<List<PrivateVoucherVO>> myPrivateVouchers() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        User me = meOptional.get();
        try {
            List<PrivateVoucherVO> list = privateVoucherService.getPrivateVouchers(me);
            return HttpResult.success(list);
        } catch (PrivateVoucherException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/redeem/{id}")
    public HttpResult<String> redeem(@PathVariable Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        try {
            boolean ok = privateVoucherService.redeemPrivateVoucher(id);
            if (ok) return HttpResult.success("Redeemed");
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Redeem Failed or Expired");
        } catch (PrivateVoucherException e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }
}
