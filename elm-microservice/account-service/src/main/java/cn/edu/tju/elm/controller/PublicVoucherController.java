package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.security.SecurityUtils;
import cn.edu.tju.elm.model.BO.Wallet;
import cn.edu.tju.elm.model.VO.PublicVoucherVO;
import cn.edu.tju.elm.repository.PrivateVoucherRepository;
import cn.edu.tju.elm.repository.WalletRepository;
import cn.edu.tju.elm.service.serviceInterface.PublicVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicVoucher")
@Tag(name = "管理公共优惠券", description = "提供平台优惠券的增删改查功能（管理员）")
public class PublicVoucherController {
  private final PublicVoucherService publicVoucherService;
  private final WalletRepository walletRepository;
  private final PrivateVoucherRepository privateVoucherRepository;

  public PublicVoucherController(
      PublicVoucherService publicVoucherService,
      WalletRepository walletRepository,
      PrivateVoucherRepository privateVoucherRepository) {
    this.publicVoucherService = publicVoucherService;
    this.walletRepository = walletRepository;
    this.privateVoucherRepository = privateVoucherRepository;
  }

  private boolean hasAdminAuthority() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(authority -> authority.getAuthority().toUpperCase())
        .anyMatch(name -> "ADMIN".equals(name) || "ROLE_ADMIN".equals(name));
  }

  @GetMapping("/list")
  @Operation(summary = "获取所有公共优惠券", description = "管理员查询所有平台优惠券")
  public HttpResult<List<PublicVoucherVO>> getAllPublicVouchers() {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    try {
      List<PublicVoucherVO> publicVoucherVOS = publicVoucherService.getPublicVouchers();
      return HttpResult.success(publicVoucherVOS);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping()
  @Operation(summary = "创建公共优惠券", description = "管理员创建新的平台优惠券")
  public HttpResult<String> createPublicVoucher(
      @Parameter(description = "公共优惠券信息", required = true) @RequestBody
          PublicVoucherVO publicVoucherVO) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
    if (failure != null) {
      return failure;
    }

    try {
      publicVoucherService.createPublicVoucher(publicVoucherVO);
      return HttpResult.success("Create Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PutMapping()
  @Operation(summary = "更新公共优惠券", description = "管理员更新平台优惠券信息")
  public HttpResult<String> updatePublicVoucher(
      @Parameter(description = "公共优惠券信息", required = true) @RequestBody
          PublicVoucherVO publicVoucherVO) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    HttpResult<String> failure = PublicVoucherVO.isValidPublicVoucherVO(publicVoucherVO);
    if (failure != null) {
      return failure;
    }

    try {
      publicVoucherService.updatePublicVoucher(publicVoucherVO);
      return HttpResult.success("Update Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("{id}")
  @Operation(summary = "根据ID获取公共优惠券", description = "管理员查询指定优惠券详情")
  public HttpResult<PublicVoucherVO> getPublicVoucher(
      @Parameter(description = "公共优惠券ID", required = true) @PathVariable Long id) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      PublicVoucherVO publicVoucherVO = publicVoucherService.getPublicVoucherById(id);
      return HttpResult.success(publicVoucherVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping("{id}")
  @Operation(summary = "删除公共优惠券", description = "管理员删除指定优惠券")
  public HttpResult<String> deletePublicVoucher(
      @Parameter(description = "公共优惠券ID", required = true) @PathVariable Long id) {
    if (!hasAdminAuthority()) {
      return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
    if (id == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "ID CANT BE NULL");
    }

    try {
      publicVoucherService.deletePublicVoucher(id);
      return HttpResult.success("Delete Public Voucher Success");
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/available")
  @Operation(summary = "获取可领取的优惠券", description = "用户查询可以领取的平台优惠券列表")
  public HttpResult<List<PublicVoucherVO>> getAvailablePublicVouchers() {
    Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "用户未登录");
    }

    try {
      Optional<Wallet> walletOpt = walletRepository.findByOwnerId(currentUserId);
      if (walletOpt.isEmpty()) {
        return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "钱包未找到");
      }
      Long walletId = walletOpt.get().getId();

      List<PublicVoucherVO> allVouchers = publicVoucherService.getPublicVouchers();

      List<PublicVoucherVO> availableVouchers =
          allVouchers.stream()
              .filter(v -> v.getClaimable() != null && v.getClaimable())
              .filter(
                  v ->
                      !privateVoucherRepository.existsByWalletIdAndPublicVoucherId(
                          walletId, v.getId()))
              .collect(Collectors.toList());

      return HttpResult.success(availableVouchers);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }
}
