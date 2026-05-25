package com.lostfound.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lostfound.backend.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
