package com.lostfound.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.backend.dto.PostPublishDTO;
import com.lostfound.backend.dto.PostQueryDTO;
import com.lostfound.backend.vo.PostVO;

public interface PostService {

    IPage<PostVO> page(PostQueryDTO query);

    PostVO detail(Long id);

    PostVO publish(Long userId, PostPublishDTO dto);

    void remove(Long id, Long userId);
}
