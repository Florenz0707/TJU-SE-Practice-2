package cn.edu.tju.address.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import cn.edu.tju.address.model.DeliveryAddress;
import cn.edu.tju.address.service.AddressInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RefreshScope
@RestController
@RequestMapping("/internal/addresses")
public class AddressInnerController {

    private final AddressInternalService addressInternalService;

    public AddressInnerController(AddressInternalService addressInternalService) {
        this.addressInternalService = addressInternalService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeliveryAddress>> byUser(@PathVariable Long userId) {
        return ResponseEntity.ok(addressInternalService.findByUserId(userId));
    }
}
