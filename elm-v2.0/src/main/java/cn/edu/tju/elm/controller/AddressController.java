package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.InternalAddressClient;
import cn.edu.tju.elm.utils.InternalOrderClient;
import cn.edu.tju.elm.utils.ResponseCompatibilityEnricher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "管理地址", description = "提供对配送地址的增删改查功能")
public class AddressController {
  private final InternalAddressClient internalAddressClient;
  private final UserService userService;
  private final ResponseCompatibilityEnricher compatibilityEnricher;

  public AddressController(
      InternalAddressClient internalAddressClient,
      UserService userService,
      ResponseCompatibilityEnricher compatibilityEnricher) {
    this.internalAddressClient = internalAddressClient;
    this.userService = userService;
    this.compatibilityEnricher = compatibilityEnricher;
  }

  @PostMapping("/addresses")
  @Operation(summary = "添加配送地址", description = "用户添加新的配送地址")
  public HttpResult<DeliveryAddress> addDeliveryAddress(
      @Parameter(description = "配送地址信息", required = true) @RequestBody DeliveryAddress address) {
    // 整体流程：Controller -> Service -> Repository
    // Controller负责方法路由和鉴权
    // Service负责数据库数据的再次处理，如比较复杂的排序、去重等
    // Repository负责与数据库的直接交互
    // 使用HttpResult进行返回响应，SUCCESS可携带实体信息，FAILURE可携带错误码（使用定义好的枚举值）

    // 通过Header: Authorization进行鉴权
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (address == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");

    InternalAddressClient.AddressSnapshot created =
      internalAddressClient.createAddress(
        new InternalAddressClient.CreateAddressCommand(
                me.getId(),
                address.getContactName(),
                address.getContactSex(),
                address.getContactTel(),
                address.getAddress()));
    if (created == null) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to create address");
    }
    DeliveryAddress createdAddress = toAddress(created);
    compatibilityEnricher.enrichAddress(createdAddress);
    return HttpResult.success(createdAddress);
  }

  @GetMapping("/addresses")
  @Operation(summary = "获取我的地址列表", description = "获取当前用户的所有配送地址")
  public HttpResult<List<DeliveryAddress>> getMyAddresses() {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    List<DeliveryAddress> myAddresses =
      internalAddressClient.getAddressesByCustomerId(me.getId()).stream()
            .map(this::toAddress)
            .toList();
    compatibilityEnricher.enrichAddresses(myAddresses);
    return HttpResult.success(myAddresses);
  }

  @PutMapping("/addresses/{id}")
  @Operation(summary = "更新配送地址", description = "完全替换指定配送地址的信息")
  public HttpResult<DeliveryAddress> updateAddress(
      @Parameter(description = "地址ID", required = true) @PathVariable("id") Long id,
      @Parameter(description = "新地址信息", required = true) @RequestBody DeliveryAddress newAddress) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    if (newAddress == null)
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");

    InternalAddressClient.AddressSnapshot existing = internalAddressClient.getAddressById(id);
    DeliveryAddress address = existing == null ? null : toAddress(existing);
    if (address == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
    Long oldCustomerId = address.getCustomerId();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.getId().equals(oldCustomerId)) {
        InternalAddressClient.AddressSnapshot updated =
          internalAddressClient.updateAddress(
              id,
            new InternalAddressClient.UpdateAddressCommand(
                  oldCustomerId,
                  newAddress.getContactName(),
                  newAddress.getContactSex(),
                  newAddress.getContactTel(),
                  newAddress.getAddress()));
      if (updated == null) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to update address");
      }
      DeliveryAddress updatedAddress = toAddress(updated);
      compatibilityEnricher.enrichAddress(updatedAddress);
      return HttpResult.success(updatedAddress);
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "删除配送地址", description = "软删除指定配送地址")
  public HttpResult<String> deleteAddress(
      @Parameter(description = "地址ID", required = true) @PathVariable("id") Long id) {
    Optional<User> meOptional = userService.getUserWithAuthorities();
    if (meOptional.isEmpty())
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
    User me = meOptional.get();

    InternalAddressClient.AddressSnapshot existing = internalAddressClient.getAddressById(id);
    DeliveryAddress address = existing == null ? null : toAddress(existing);
    if (address == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
    Long customerId = address.getCustomerId();

    boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
    if (isAdmin || me.getId().equals(customerId)) {
      boolean deleted = internalAddressClient.deleteAddress(id);
      if (!deleted) {
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "Failed to delete address");
      }
      return HttpResult.success("Delete address successfully.");
    }
    return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
  }

  private DeliveryAddress toAddress(InternalAddressClient.AddressSnapshot snapshot) {
    DeliveryAddress address = new DeliveryAddress();
    address.setId(snapshot.id());
    address.setCustomerId(snapshot.customerId());
    address.setContactName(snapshot.contactName());
    address.setContactSex(snapshot.contactSex());
    address.setContactTel(snapshot.contactTel());
    address.setAddress(snapshot.address());
    return address;
  }
}
