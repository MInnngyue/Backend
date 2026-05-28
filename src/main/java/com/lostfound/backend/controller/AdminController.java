package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final MessageMapper messageMapper;

    private User requireAdmin(Authentication auth) {
        User user = (User) auth.getPrincipal();
        if (user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return user;
    }

    @GetMapping("/posts/pending")
    public Result<Page<Post>> pendingPosts(Authentication auth,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer reviewStatus) {
        requireAdmin(auth);
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(reviewStatus != null, Post::getReviewStatus, reviewStatus)
                .eq(reviewStatus == null, Post::getReviewStatus, 0)
                .orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectPage(p, wrapper));
    }

    @PutMapping("/posts/{id}/approve")
    public Result<Void> approve(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setReviewStatus(1);
        postMapper.updateById(post);
        return Result.success(null);
    }

    @PutMapping("/posts/{id}/reject")
    public Result<Void> reject(Authentication auth, @PathVariable Long id, @RequestParam(defaultValue = "违规内容") String reason) {
        requireAdmin(auth);
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setReviewStatus(2);
        post.setReviewRemark(reason);
        post.setStatus(5); // 已下架
        postMapper.updateById(post);
        return Result.success(null);
    }

    @GetMapping("/users")
    public Result<Page<User>> users(Authentication auth,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        requireAdmin(auth);
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        return Result.success(userMapper.selectPage(p, wrapper));
    }

    @PutMapping("/users/{id}/freeze")
    public Result<Void> freezeUser(Authentication auth, @PathVariable Long id, @RequestParam(defaultValue = "0") int days) {
        requireAdmin(auth);
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (user.getStatus() == 1) {
            user.setStatus(0);
            user.setBanUntil(null);
        } else {
            user.setStatus(1);
            if (days > 0) user.setBanUntil(java.time.LocalDateTime.now().plusDays(days));
        }
        userMapper.updateById(user);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/blacklist")
    public Result<Void> blacklistUser(Authentication auth, @PathVariable Long id, @RequestParam(defaultValue = "0") int days) {
        requireAdmin(auth);
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (user.getBlacklisted() != null && user.getBlacklisted() == 1) {
            user.setBlacklisted(0);
            user.setBlacklistUntil(null);
        } else {
            user.setBlacklisted(1);
            if (days > 0) user.setBlacklistUntil(java.time.LocalDateTime.now().plusDays(days));
        }
        userMapper.updateById(user);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/credit")
    public Result<Void> adjustCredit(Authentication auth, @PathVariable Long id, @RequestParam int delta) {
        requireAdmin(auth);
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        int newScore = Math.max(0, Math.min(100, user.getCreditScore() + delta));
        user.setCreditScore(newScore);
        userMapper.updateById(user);
        return Result.success(null);
    }

    @GetMapping("/dict")
    public Result<List<Category>> dictList(Authentication auth, @RequestParam String type) {
        requireAdmin(auth);
        return Result.success(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getType, type).orderByAsc(Category::getSortOrder)));
    }

    @PostMapping("/dict")
    public Result<Category> dictAdd(Authentication auth, @RequestBody Category category) {
        requireAdmin(auth);
        categoryMapper.insert(category);
        return Result.success(category);
    }

    @PutMapping("/dict/{id}")
    public Result<Void> dictUpdate(Authentication auth, @PathVariable Long id, @RequestBody Category category) {
        requireAdmin(auth);
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success(null);
    }

    @DeleteMapping("/dict/{id}")
    public Result<Void> dictDelete(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        categoryMapper.deleteById(id);
        return Result.success(null);
    }

    @GetMapping("/posts/all")
    public Result<Page<Post>> allPosts(Authentication auth,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        requireAdmin(auth);
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(status != null, Post::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                    .like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getDescription, keyword))
                .orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectPage(p, wrapper));
    }

    @PutMapping("/posts/{id}/archive")
    public Result<Void> archive(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setStatus(4);
        postMapper.updateById(post);
        return Result.success(null);
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(Authentication auth, @PathVariable Long id) {
        requireAdmin(auth);
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        postMapper.deleteById(id);
        return Result.success(null);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(Authentication auth) {
        requireAdmin(auth);
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("totalPosts", postMapper.selectCount(null));
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("activePosts", postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 0)));
        data.put("completedPosts", postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 3)));
        data.put("matchedPosts", postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 1)));
        data.put("pendingReviews", postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getReviewStatus, 0)));

        // 各类别帖子数
        List<Map<String, Object>> categoryStats = new ArrayList<>();
        List<Category> itemCats = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getType, "item_category").eq(Category::getParentId, 0L));
        for (Category cat : itemCats) {
            long count = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                    .eq(Post::getItemCategory, cat.getName()));
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat.getName());
            item.put("count", count);
            categoryStats.add(item);
        }
        data.put("categoryDistribution", categoryStats);

        return Result.success(data);
    }
}
