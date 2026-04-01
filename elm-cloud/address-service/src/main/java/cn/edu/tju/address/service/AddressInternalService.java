package cn.edu.tju.address.service;

import cn.edu.tju.address.model.DeliveryAddress;
import java.util.List;

public interface AddressInternalService {
    List<DeliveryAddress> findByUserId(Long userId);
}
