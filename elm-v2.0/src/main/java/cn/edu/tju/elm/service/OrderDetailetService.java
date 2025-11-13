package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.BO.OrderDetailet;
import cn.edu.tju.elm.repository.OrderDetailetRepository;
import cn.edu.tju.elm.utils.EntityUtils;
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
        return EntityUtils.filterEntityList(orderDetailetRepository.findAllByOrderId(orderId));
    }
}
