package com.lostfound.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lostfound.backend.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {
}
