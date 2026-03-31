package cn.edu.tju.address.controller;

import cn.edu.tju.address.model.vo.DeliveryAddressSnapshotVO;
import cn.edu.tju.address.service.AddressInternalService;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/address")
@Tag(name = "地址内部接口", description = "address-service 地址域内部接口")
public class AddressInnerController {
  private final AddressInternalService addressInternalService;

  public AddressInnerController(AddressInternalService addressInternalService) {
    this.addressInternalService = addressInternalService;
  }

  @PostMapping("")
  @Operation(summary = "创建地址", description = "创建用户配送地址")
  public HttpResult<DeliveryAddressSnapshotVO> createAddress(@RequestBody AddressUpsertRequest request) {
    if (request == null || request.getCustomerId() == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "CustomerId CANT BE NULL");
    }
    try {
      DeliveryAddressSnapshotVO created =
          addressInternalService.createAddress(
              new AddressInternalService.CreateAddressCommand(
                  request.getCustomerId(),
                  request.getContactName(),
                  request.getContactSex(),
                  request.getContactTel(),
                  request.getAddress()));
      return HttpResult.success(created);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @GetMapping("/{addressId}")
  @Operation(summary = "查询地址", description = "按地址ID查询")
  public HttpResult<DeliveryAddressSnapshotVO> getAddressById(
      @Parameter(description = "地址ID", required = true) @PathVariable("addressId") Long addressId) {
    DeliveryAddressSnapshotVO address = addressInternalService.getAddressById(addressId);
    if (address == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
    }
    return HttpResult.success(address);
  }

  @GetMapping("/customer/{customerId}")
  @Operation(summary = "查询用户地址列表", description = "按用户ID查询地址列表")
  public HttpResult<List<DeliveryAddressSnapshotVO>> getAddressesByCustomerId(
      @Parameter(description = "用户ID", required = true) @PathVariable("customerId") Long customerId) {
    return HttpResult.success(addressInternalService.getAddressesByCustomerId(customerId));
  }

  @PutMapping("/{addressId}")
  @Operation(summary = "更新地址", description = "更新地址信息")
  public HttpResult<DeliveryAddressSnapshotVO> updateAddress(
      @Parameter(description = "地址ID", required = true) @PathVariable("addressId") Long addressId,
      @RequestBody AddressUpsertRequest request) {
    if (request == null) {
      return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");
    }
    try {
      DeliveryAddressSnapshotVO updated =
          addressInternalService.updateAddress(
              addressId,
              new AddressInternalService.UpdateAddressCommand(
                  request.getCustomerId(),
                  request.getContactName(),
                  request.getContactSex(),
                  request.getContactTel(),
                  request.getAddress()));
      return HttpResult.success(updated);
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  @DeleteMapping("/{addressId}")
  @Operation(summary = "删除地址", description = "软删除地址")
  public HttpResult<Boolean> deleteAddress(
      @Parameter(description = "地址ID", required = true) @PathVariable("addressId") Long addressId) {
    try {
      return HttpResult.success(addressInternalService.deleteAddress(addressId));
    } catch (Exception e) {
      return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, e.getMessage());
    }
  }

  public static class AddressUpsertRequest {
    private Long customerId;
    private String contactName;
    private Integer contactSex;
    private String contactTel;
    private String address;

    public Long getCustomerId() {
      return customerId;
    }

    public void setCustomerId(Long customerId) {
      this.customerId = customerId;
    }

    public String getContactName() {
      return contactName;
    }

    public void setContactName(String contactName) {
      this.contactName = contactName;
    }

    public Integer getContactSex() {
      return contactSex;
    }

    public void setContactSex(Integer contactSex) {
      this.contactSex = contactSex;
    }

    public String getContactTel() {
      return contactTel;
    }

    public void setContactTel(String contactTel) {
      this.contactTel = contactTel;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }
}