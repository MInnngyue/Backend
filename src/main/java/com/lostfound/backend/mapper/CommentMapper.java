package com.lostfound.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lostfound.backend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
