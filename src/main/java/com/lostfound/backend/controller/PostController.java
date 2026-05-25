package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.dto.PostPublishDTO;
import com.lostfound.backend.dto.PostQueryDTO;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.service.CreditScoreService;
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
    private final CreditScoreService creditScoreService;

    @GetMapping
    public Result<Page<PostVO>> list(PostQueryDTO query) {
        return Result.success(postService.page(query));
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.success(postService.detail(id));
    }

    @PostMapping
    public Result<PostVO> publish(@Valid @RequestBody PostPublishDTO dto, Authentication auth) {
        User user = (User) auth.getPrincipal();
        if (!creditScoreService.canPublish(user)) {
            throw new BusinessException(403, "信用分不足60，无法发布帖子");
        }
        return Result.success(postService.publish(user.getId(), dto));
    }

    /** 手动标记帖子为已完成（发布者操作） */
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();
        postService.complete(id, user.getId());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        postService.remove(id, userId);
        return Result.success(null);
    }
}
