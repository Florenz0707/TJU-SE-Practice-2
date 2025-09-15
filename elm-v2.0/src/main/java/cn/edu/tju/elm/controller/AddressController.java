package cn.edu.tju.elm.controller;

import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.security.service.UserService;
import cn.edu.tju.elm.model.DeliveryAddress;
//import cn.edu.tju.elb.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name="管理地址", description = "对配送地址的增删改查")
public class AddressController {
//    @Autowired
//    private AddressService addressService;

    @Autowired
    private UserService userService;

    @PostMapping("/addresses")
    public HttpResult<DeliveryAddress> addDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress) {

        return null;
    }
}
