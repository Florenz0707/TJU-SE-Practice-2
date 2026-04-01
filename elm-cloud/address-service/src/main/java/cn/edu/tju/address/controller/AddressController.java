package cn.edu.tju.address.controller;

import cn.edu.tju.address.model.DeliveryAddress;
import cn.edu.tju.address.repository.DeliveryAddressRepository;
import cn.edu.tju.core.model.HttpResult;
import cn.edu.tju.core.model.ResultCodeEnum;
import cn.edu.tju.address.util.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final DeliveryAddressRepository repository;
    private final JwtUtils jwtUtils;

    public AddressController(DeliveryAddressRepository repository, JwtUtils jwtUtils) {
        this.repository = repository;
        this.jwtUtils = jwtUtils;
    }

    private Long verifyUser(String token) {
        return jwtUtils.getUserIdFromToken(token);
    }

    @PostMapping
    public HttpResult<DeliveryAddress> addAddress(@RequestHeader(value = "Authorization", required = false) String token,
                                                  @RequestBody DeliveryAddress address) {
        Long userId = verifyUser(token);
        if (userId == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        if (address == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");
        }
        address.setCustomerId(userId);
        DeliveryAddress saved = repository.save(address);
        return HttpResult.success(saved);
    }

    @GetMapping
    public HttpResult<List<DeliveryAddress>> getMyAddresses(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = verifyUser(token);
        if (userId == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        List<DeliveryAddress> addresses = repository.findByCustomerId(userId);
        return HttpResult.success(addresses);
    }

    @PutMapping("/{id}")
    public HttpResult<DeliveryAddress> updateAddress(@RequestHeader(value = "Authorization", required = false) String token,
                                                     @PathVariable("id") Long id,
                                                     @RequestBody DeliveryAddress address) {
        Long userId = verifyUser(token);
        if (userId == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        if (address == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address CANT BE NULL");
        }
        Optional<DeliveryAddress> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        }
        DeliveryAddress existing = opt.get();
        if (!existing.getCustomerId().equals(userId)) {
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        
        existing.setContactName(address.getContactName());
        existing.setContactSex(address.getContactSex());
        existing.setContactTel(address.getContactTel());
        existing.setAddress(address.getAddress());
        
        DeliveryAddress saved = repository.save(existing);
        return HttpResult.success(saved);
    }

    @DeleteMapping("/{id}")
    public HttpResult<String> deleteAddress(@RequestHeader(value = "Authorization", required = false) String token,
                                            @PathVariable("id") Long id) {
        Long userId = verifyUser(token);
        if (userId == null) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "AUTHORITY NOT FOUND");
        }
        Optional<DeliveryAddress> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return HttpResult.failure(ResultCodeEnum.NOT_FOUND, "Address NOT FOUND");
        }
        DeliveryAddress existing = opt.get();
        if (!existing.getCustomerId().equals(userId)) {
            // we ignore isAdmin check since microservices usually handle this via api gateway or simply forbid it.
            return HttpResult.failure(ResultCodeEnum.FORBIDDEN, "AUTHORITY LACKED");
        }
        
        repository.deleteById(id);
        return HttpResult.success("Delete address successfully.");
    }
}
