package com.lostfound.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.entity.Category;
import com.lostfound.backend.mapper.CategoryMapper;
import com.lostfound.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> listByType(String type) {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getType, type)
                .eq(Category::getParentId, 0L)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    @Override
    public List<Category> listByParentId(Long parentId) {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder));
    }

    @Override
    public Category add(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category update(Category category) {
        Category exist = categoryMapper.selectById(category.getId());
        if (exist == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}
