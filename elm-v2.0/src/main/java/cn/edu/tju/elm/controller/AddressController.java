package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.service.AddressService;
import cn.edu.tju.elm.utils.Utils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理地址", description = "对配送地址的增删改查")
public class AddressController {

    // 准备需要的接口
    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    @PostMapping("/addresses")
    public HttpResult<DeliveryAddress> addDeliveryAddress(@RequestBody DeliveryAddress address) {
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

        address.setCustomer(me);
        Utils.setNewEntity(address, me);
        addressService.addAddress(address);

        return HttpResult.success(address);
    }

    @GetMapping("/addresses")
    public HttpResult<List<DeliveryAddress>> getMyAddresses() {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        List<DeliveryAddress> myAddresses = addressService.getAddressesByCustomerId(me.getId());
        return HttpResult.success(myAddresses);
    }

    @PutMapping("/addresses/{id}")
    public HttpResult<DeliveryAddress> updateAddress(
            @PathVariable("id") Long id,
            @RequestBody DeliveryAddress address) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");

        if (address.getCustomer() == null || address.getCustomer().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer.Id CANT BE NULL");
        User customer = userService.getUserById(address.getCustomer().getId());
        if (customer == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer NOT FOUND");

        DeliveryAddress oldAddress = addressService.getAddressById(id);
        if (oldAddress == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        User oldCustomer = oldAddress.getCustomer();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || (me.equals(oldCustomer) && me.equals(customer))) {
            address.setCustomer(customer);
            Utils.substituteEntity(oldAddress, address, me);
            addressService.updateAddress(oldAddress);
            addressService.updateAddress(address);

            userService.updateUser(customer);
            return HttpResult.success(address);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<Object> deleteAddress(@PathVariable("id") Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        DeliveryAddress address = addressService.getAddressById(id);
        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        User customer = address.getCustomer();

        boolean isAdmin = Utils.hasAuthority(me, "ADMIN");
        if (isAdmin || (me.equals(customer))) {
            Utils.deleteEntity(address, me);
            addressService.updateAddress(address);
            return HttpResult.success(address);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
