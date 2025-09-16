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

import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "管理地址", description = "对配送地址的增删改查")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/addresses")
    public HttpResult<DeliveryAddress> addDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress) {
        Optional<User> meOptional = userService.getUserWithAuthorities();
        if (meOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);

        User me = meOptional.get();
        Optional<User> userOptional = userRepository.findOneWithAuthoritiesByUsername(deliveryAddress.getCustomer().getUsername());
        if (userOptional.isEmpty()) return HttpResult.failure(ResultCodeEnum.NOT_FOUND);

        User user = userOptional.get();
        if (me.getUsername().equals(user.getUsername())) {
            deliveryAddress.setCustomer(user);
            addressService.addAddress(deliveryAddress);
            return HttpResult.success(deliveryAddress);
        }
        return HttpResult.failure(ResultCodeEnum.SERVER_ERROR);
    }
}
