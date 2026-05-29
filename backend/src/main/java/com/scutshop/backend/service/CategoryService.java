package com.scutshop.backend.service;

import com.scutshop.backend.mapper.CategoryMapper;
import com.scutshop.backend.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper mapper;

    public CategoryService(CategoryMapper mapper) {
        this.mapper = mapper;
    }

    public List<Category> listAll() {
        return mapper.selectAll();
    }

    public List<Category> listRoot() {
        return mapper.selectRootCategories();
    }

    public List<Category> listByParent(Long parentId) {
        return mapper.selectByParentId(parentId);
    }

    public Category findById(Long id) {
        return mapper.selectById(id);
    }

    public int create(Category c) {
        return mapper.insert(c);
    }

    public int update(Category c) {
        return mapper.update(c);
    }

    public int delete(Long id) {
        return mapper.delete(id);
    }
}
