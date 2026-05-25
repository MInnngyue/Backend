package com.lostfound.backend.controller;

import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.Category;
import com.lostfound.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{type}")
    public Result<List<Category>> listByType(@PathVariable String type) {
        return Result.success(categoryService.listByType(type));
    }

    @GetMapping("/{type}/children")
    public Result<List<Category>> listChildren(@PathVariable String type, @RequestParam Long parentId) {
        return Result.success(categoryService.listByParentId(parentId));
    }
}
