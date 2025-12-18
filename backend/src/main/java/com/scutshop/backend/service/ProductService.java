package com.scutshop.backend.service;

import com.scutshop.backend.mapper.ProductMapper;
import com.scutshop.backend.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductMapper mapper;

    public ProductService(ProductMapper mapper) {
        this.mapper = mapper;
    }

    public Product findById(Long id) {
        return mapper.selectById(id);
    }

    public List<Product> search(String q, int page, int size) {
        int limit = size;
        int offset = (Math.max(1, page) - 1) * size;
        return mapper.search(q, limit, offset);
    }

    public int count(String q) {
        return mapper.count(q);
    }

    public int create(Product p) {
        return mapper.insert(p);
    }

    public int update(Product p) {
        return mapper.update(p);
    }

    public int delete(Long id) {
        return mapper.delete(id);
    }

    public int decrementStock(Long productId, int quantity) {
        return mapper.decrementStock(productId, quantity);
    }
}
