package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.dto.PostPublishDTO;
import com.lostfound.backend.dto.PostQueryDTO;
import com.lostfound.backend.service.PostService;
import com.lostfound.backend.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public Result<IPage<PostVO>> list(PostQueryDTO query) {
        return Result.success(postService.page(query));
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.success(postService.detail(id));
    }

    @PostMapping
    public Result<PostVO> publish(@Valid @RequestBody PostPublishDTO dto, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return Result.success(postService.publish(userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        postService.remove(id, userId);
        return Result.success(null);
    }
}
