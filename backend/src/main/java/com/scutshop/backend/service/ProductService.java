package com.scutshop.backend.service;

import com.scutshop.backend.mapper.ProductMapper;
import com.scutshop.backend.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductMapper mapper;
    private final com.scutshop.backend.mapper.ProductAuditMapper auditMapper;

    public ProductService(ProductMapper mapper, com.scutshop.backend.mapper.ProductAuditMapper auditMapper) {
        this.mapper = mapper;
        this.auditMapper = auditMapper;
    }

    public Product findById(Long id) {
        return mapper.selectById(id);
    }

    public List<Product> search(String q, int page, int size, Integer status) {
        int limit = size;
        int offset = (Math.max(1, page) - 1) * size;
        return mapper.search(q, limit, offset, status);
    }

    public int count(String q, Integer status) {
        return mapper.count(q, status);
    }

    public int create(Product p) {
        return mapper.insert(p);
    }

    public int update(Product p) {
        return mapper.update(p);
    }

    public int delete(Long id, String actor) {
        // soft delete: set status = 0 and record audit
        int n = mapper.setStatus(id, 0);
        try {
            // record audit if possible
            auditMapper.insert(id, "down", actor, "soft delete by admin");
        } catch (Exception e) {
            // ignore audit failures
        }
        return n;
    }

    public int restore(Long id, String actor) {
        int n = mapper.setStatus(id, 1);
        try {
            auditMapper.insert(id, "restore", actor, "restore by admin");
        } catch (Exception e) {
        }
        return n;
    }

    public int decrementStock(Long productId, int quantity) {
        return mapper.decrementStock(productId, quantity);
    }
}
