package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.OrderDetailet;
import cn.edu.tju.elm.repository.OrderDetailetRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderDetailetService {

    private final OrderDetailetRepository orderDetailetRepository;

    public OrderDetailetService(OrderDetailetRepository orderDetailetRepository) {
        this.orderDetailetRepository = orderDetailetRepository;
    }

    public void addOrderDetailet(OrderDetailet orderDetailet) {
        orderDetailetRepository.save(orderDetailet);
    }

    public List<OrderDetailet> getOrderDetailetsByOrderId(Long orderId) {
        return Utils.checkEntityList(orderDetailetRepository.findAllByOrderId(orderId));
    }
}
