package com.lostfound.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    IPage<Post> selectPostPage(Page<Post> page,
                                @Param("type") Integer type,
                                @Param("itemCategory") String itemCategory,
                                @Param("status") Integer status,
                                @Param("keyword") String keyword);
}
