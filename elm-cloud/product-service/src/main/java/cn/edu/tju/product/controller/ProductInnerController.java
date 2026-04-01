package cn.edu.tju.product.controller;

import cn.edu.tju.product.model.Product;
import cn.edu.tju.product.service.ProductInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/products")
public class ProductInnerController {

    private final ProductInternalService productInternalService;

    public ProductInnerController(ProductInternalService productInternalService) {
        this.productInternalService = productInternalService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> all() {
        return ResponseEntity.ok(productInternalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return productInternalService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
