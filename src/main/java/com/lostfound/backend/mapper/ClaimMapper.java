package com.lostfound.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lostfound.backend.entity.Claim;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClaimMapper extends BaseMapper<Claim> {
}
