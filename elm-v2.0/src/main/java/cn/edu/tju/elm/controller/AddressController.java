package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.core.model.User;
import cn.edu.tju.core.security.repository.UserRepository;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.DeliveryAddress;
import cn.edu.tju.elm.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/addresses")
    public HttpResult<DeliveryAddress> addDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress) {
        // 整体流程：Controller -> Service -> Repository
        // Controller负责方法路由和鉴权
        // Service负责数据库数据的再次处理，如比较复杂的排序、去重等
        // Repository负责与数据库的直接交互

        // 使用HttpResult进行返回响应，SUCCESS可携带实体信息，FAILURE可携带错误码（使用定义好的枚举值）
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User me = meOptional.get();

        Optional<User> userOptional = userRepository.findOneWithAuthoritiesByUsername(deliveryAddress.getCustomer().getUsername());
        if (userOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);
        User user = userOptional.get();

        // 检查token指向的user是否与deliveryAddress中的customer一致
        if (me.getUsername().equals(user.getUsername())) {
            LocalDateTime now = LocalDateTime.now();
            deliveryAddress.setCreateTime(now);
            deliveryAddress.setUpdateTime(now);
            deliveryAddress.setCreator(user.getId());
            deliveryAddress.setUpdater(user.getId());
            deliveryAddress.setDeleted(false);
            deliveryAddress.setCustomer(user);  // 使user被jpa接管，否则会报错
            if (deliveryAddress.equals(addressService.addAddress(deliveryAddress)))
                return HttpResult.success(deliveryAddress);
        }
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }
}
