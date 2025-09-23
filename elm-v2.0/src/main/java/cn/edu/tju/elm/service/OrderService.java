package cn.edu.tju.elm.service;

import cn.edu.tju.elm.model.Order;
import cn.edu.tju.elm.repository.OrderRepository;
import cn.edu.tju.elm.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void addOrder(Order order) {
        orderRepository.save(order);
    }

    public Order getOrderById(Long id) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        return orderOptional.map(Utils::checkEntity).orElse(null);
    }

    public List<Order> getOrdersByCustomerId(Long id) {
        return Utils.checkEntityList(orderRepository.findAllByCustomerId(id));
    }

    public void updateOrder(Order order) {
        orderRepository.save(order);
    }
}
