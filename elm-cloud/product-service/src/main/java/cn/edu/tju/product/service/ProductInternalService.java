package cn.edu.tju.product.service;

import cn.edu.tju.product.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductInternalService {
    Optional<Product> findById(Long id);
    List<Product> findAll();
}
