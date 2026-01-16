package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/publicVoucher")
public class PublicVoucherController {
    private final PublicVoucherService publicVoucherService;
    private final UserService userService;
    private final WalletRepository walletRepository;
    private final PrivateVoucherRepository privateVoucherRepository;

    public PublicVoucherController(
            @Qualifier("publicVoucherServiceImpl") PublicVoucherService publicVoucherServiceImpl,
            UserService userService,
            WalletRepository walletRepository,
            PrivateVoucherRepository privateVoucherRepository) {
        this.publicVoucherService = publicVoucherServiceImpl;
        this.userService = userService;
        this.walletRepository = walletRepository;
        this.privateVoucherRepository = privateVoucherRepository;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<PublicVoucherVO>> getAllPublicVouchers() {
        try {
            List<PublicVoucherVO> publicVoucherVOS = publicVoucherService.getPublicVouchers();
            return HttpResult.success(publicVoucherVOS);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<String> createPublicVoucher(
            @RequestBody PublicVoucherVO publicVoucherVO) {
        HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
        if (failure != null) return failure;

        try {
            publicVoucherService.createPublicVoucher(publicVoucherVO);
            return HttpResult.success("Create Public Voucher Success");
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @PutMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<String> updatePublicVoucher(
            @RequestBody PublicVoucherVO publicVoucherVO) {
        HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
        if (failure != null) return failure;

        try {
            publicVoucherService.updatePublicVoucher(publicVoucherVO);
            return HttpResult.success("Update Public Voucher Success");
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<PublicVoucherVO> getPublicVoucher(
            @PathVariable Long id) {
        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

        try {
            PublicVoucherVO publicVoucherVO = publicVoucherService.getPublicVoucherById(id);
            return HttpResult.success(publicVoucherVO);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<String> deletePublicVoucher(
            @PathVariable Long id) {
        if (id == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");

        try {
            publicVoucherService.deletePublicVoucher(id);
            return HttpResult.success("Delete Public Voucher Success");
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/available")
    public HttpResult<List<PublicVoucherVO>> getAvailablePublicVouchers() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
        }
        User me = meOptional.get();

        try {
            // Get user's wallet
            Optional<Wallet> walletOpt = walletRepository.findByOwnerId(me.getId());
            if (walletOpt.isEmpty()) {
                return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "钱包未找到");
            }
            Long walletId = walletOpt.get().getId();

            // Get all public vouchers
            List<PublicVoucherVO> allVouchers = publicVoucherService.getPublicVouchers();
            
            // Filter: only claimable vouchers that user hasn't claimed yet
            List<PublicVoucherVO> availableVouchers = allVouchers.stream()
                    .filter(v -> v.getClaimable() != null && v.getClaimable())
                    .filter(v -> !privateVoucherRepository.existsByWalletIdAndPublicVoucherId(walletId, v.getId()))
                    .collect(Collectors.toList());
            
            return HttpResult.success(availableVouchers);
        } catch (Exception e) {
            return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
        }
    }
}
