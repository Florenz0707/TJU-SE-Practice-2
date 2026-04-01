package cn.edu.tju.product.service.impl;

import cn.edu.tju.product.model.Product;
import cn.edu.tju.product.repository.ProductRepository;
import cn.edu.tju.product.service.ProductInternalService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductInternalServiceImpl implements ProductInternalService {

    private final ProductRepository productRepository;

    public ProductInternalServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }
}
