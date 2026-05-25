package com.lostfound.backend.service;

import com.lostfound.backend.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> listByType(String type);

    List<Category> listByParentId(Long parentId);

    Category add(Category category);

    Category update(Category category);

    void delete(Long id);
}
