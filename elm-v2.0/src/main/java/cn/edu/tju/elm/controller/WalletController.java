package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.VO.WalletVO;
import cn.edu.tju.elm.service.serviceInterface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "管理钱包", description = "提供钱包的创建和查询功能")
public class WalletController {
  private final UserService userService;
  private final WalletService walletService;

  public WalletController(
      UserService userService, @Qualifier("walletServiceImpl") WalletService walletServiceImpl) {
    this.userService = userService;
    this.walletService = walletServiceImpl;
  }

  @GetMapping("/owner/{id}")
  @Operation(summary = "根据钱包ID获取所有者", description = "查询指定钱包的所有者信息")
  public HttpResult<User> getWalletOwnerById(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

    try {
      User owner = walletService.getWalletOwnerById(id);
      return HttpResult.success(owner);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("{id}")
  @Operation(summary = "根据ID获取钱包", description = "查询指定钱包的详细信息")
  public HttpResult<WalletVO> getWalletById(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (id == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "WalletId CANT BE NULL");

    try {
      WalletVO walletVO = walletService.getWalletById(id, me);
      return HttpResult.success(walletVO);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @PostMapping("/voucher/{walletId}")
  @Operation(summary = "添加优惠券到钱包", description = "向指定钱包添加优惠券")
  public HttpResult<String> addVoucher(
      @Parameter(description = "钱包ID", required = true) @PathVariable Long walletId,
      @Parameter(description = "优惠券金额", required = true) @RequestBody BigDecimal amount) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");

    if (amount == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Amount CANT BE NULL");
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
  @Operation(summary = "获取我的钱包", description = "获取当前用户的钱包信息")
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
  @Operation(summary = "创建钱包", description = "为当前用户创建新钱包")
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
