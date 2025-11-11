package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.BO.DeliveryAddress;
import cn.edu.tju.elm.service.AddressService;
import cn.edu.tju.elm.utils.AuthorityUtils;
import cn.edu.tju.elm.utils.EntityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理地址", description = "对配送地址的增删改查")
public class AddressController {
    private final AddressService addressService;
    private final UserService userService;

    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

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
        EntityUtils.setNewEntity(address, me);
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
            @RequestBody DeliveryAddress newAddress) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (newAddress == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");

        DeliveryAddress address = addressService.getAddressById(id);
        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        User oldCustomer = address.getCustomer();

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || me.equals(oldCustomer)) {
            newAddress.setCustomer(oldCustomer);
            newAddress.setId(null);
            EntityUtils.substituteEntity(address, newAddress, me);
            addressService.updateAddress(address);
            addressService.updateAddress(newAddress);
            return HttpResult.success(newAddress);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }

    @DeleteMapping("/{id}")
    public HttpResult<String> deleteAddress(@PathVariable("id") Long id) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        DeliveryAddress address = addressService.getAddressById(id);
        if (address == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        User customer = address.getCustomer();

        boolean isAdmin = AuthorityUtils.hasAuthority(me, "ADMIN");
        if (isAdmin || (me.equals(customer))) {
            EntityUtils.deleteEntity(address, me);
            addressService.updateAddress(address);
            return HttpResult.success("Delete address successfully.");
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
