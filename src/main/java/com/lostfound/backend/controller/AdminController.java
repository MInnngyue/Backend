package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import lombok.RequiredArgsConstructor;
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

    // ============ 权限检查 ============
    private void checkAdmin(User user) {
        if (user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    // ============ 帖子审核 ============

    /** 待审核帖子列表 */
    @GetMapping("/posts/pending")
    public Result<Page<Post>> pendingPosts(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer reviewStatus) {
        Page<Post> p = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(reviewStatus != null, Post::getReviewStatus, reviewStatus)
                .eq(reviewStatus == null, Post::getReviewStatus, 0)
                .orderByDesc(Post::getCreateTime);
        return Result.success(postMapper.selectPage(p, wrapper));
    }

    /** 审核通过 */
    @PutMapping("/posts/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setReviewStatus(1);
        postMapper.updateById(post);
        return Result.success(null);
    }

    /** 审核拒绝 */
    @PutMapping("/posts/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(defaultValue = "违规内容") String reason) {
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        post.setReviewStatus(2);
        post.setReviewRemark(reason);
        post.setStatus(5); // 已下架
        postMapper.updateById(post);
        return Result.success(null);
    }

    // ============ 用户管理 ============

    @GetMapping("/users")
    public Result<Page<User>> users(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        return Result.success(userMapper.selectPage(p, wrapper));
    }

    @PutMapping("/users/{id}/freeze")
    public Result<Void> freezeUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(user);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/credit")
    public Result<Void> adjustCredit(@PathVariable Long id, @RequestParam int delta) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        int newScore = Math.max(0, Math.min(120, user.getCreditScore() + delta));
        user.setCreditScore(newScore);
        userMapper.updateById(user);
        return Result.success(null);
    }

    // ============ 数据字典 ============

    @GetMapping("/dict")
    public Result<List<Category>> dictList(@RequestParam String type) {
        return Result.success(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getType, type).orderByAsc(Category::getSortOrder)));
    }

    @PostMapping("/dict")
    public Result<Category> dictAdd(@RequestBody Category category) {
        categoryMapper.insert(category);
        return Result.success(category);
    }

    @PutMapping("/dict/{id}")
    public Result<Void> dictUpdate(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success(null);
    }

    @DeleteMapping("/dict/{id}")
    public Result<Void> dictDelete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.success(null);
    }

    // ============ 数据统计 ============

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
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
