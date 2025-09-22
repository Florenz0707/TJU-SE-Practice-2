package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.Authority;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public HttpResult<DeliveryAddress> addDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress) {
        // 整体流程：Controller -> Service -> Repository
        // Controller负责方法路由和鉴权
        // Service负责数据库数据的再次处理，如比较复杂的排序、去重等
        // Repository负责与数据库的直接交互
        // 使用HttpResult进行返回响应，SUCCESS可携带实体信息，FAILURE可携带错误码（使用定义好的枚举值）

        // 通过Header: Authorization进行鉴权
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        // 检查参数关键数据是否为空，以及是否有效
        if (deliveryAddress.getCustomer() == null || deliveryAddress.getCustomer().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer.Id NOT FOUND");
        User user = userService.getUserById(deliveryAddress.getCustomer().getId());
        if (user == null) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);

        // 检查token指向的user是否与deliveryAddress中的customer一致
        if (me.getUsername().equals(user.getUsername())) {
            // 使user被jpa接管，否则会报错
            deliveryAddress.setCustomer(user);
            // 注意：当且仅当user对jpa来说是“陌生”的时候需要，如直接在外部通过sql脚本执行插入

            LocalDateTime now = LocalDateTime.now();
            deliveryAddress.setCreateTime(now);
            deliveryAddress.setUpdateTime(now);
            deliveryAddress.setCreator(user.getId());
            deliveryAddress.setUpdater(user.getId());
            deliveryAddress.setDeleted(false);
            if (deliveryAddress.equals(addressService.addAddress(deliveryAddress)))
                return HttpResult.success(deliveryAddress);
        }
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR, "UNKNOWN ERROR");
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

    @PutMapping("/{id}")
    public HttpResult<DeliveryAddress> updateAddress(
            @PathVariable("id") Long id,
            @RequestBody DeliveryAddress address) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty())
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        User me = meOptional.get();

        if (address.getCustomer() == null || address.getCustomer().getId() == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer.Id CANT BE NULL");
        User customer = userService.getUserById(address.getCustomer().getId());
        if (customer == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Customer NOT FOUND");

        DeliveryAddress oldAddress = addressService.getAddressById(id);
        if (oldAddress == null)
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        User oldCustomer = oldAddress.getCustomer();

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        if (isAdmin || (me.equals(oldCustomer) && me.equals(customer))) {
            address.setId(oldAddress.getId());
            address.setCustomer(customer);

            LocalDateTime now = LocalDateTime.now();
            address.setCreateTime(oldAddress.getCreateTime());
            address.setUpdateTime(now);
            address.setCreator(oldAddress.getCreator());
            address.setUpdater(me.getId());
            address.setDeleted(false);
            addressService.updateAddress(address);
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

        boolean isAdmin = false;
        for (Authority authority : me.getAuthorities()) {
            if (authority.getName().equals("ADMIN")) {
                isAdmin = true;
                break;
            }
        }

        if (isAdmin || (me.equals(address.getCustomer()))) {
            address.setDeleted(true);

            LocalDateTime now = LocalDateTime.now();
            address.setUpdateTime(now);
            address.setUpdater(me.getId());
            addressService.updateAddress(address);
            return HttpResult.success(address);
        }
        return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
    }
}
