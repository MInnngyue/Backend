package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @GetMapping("/post/{postId}")
    public Result<List<Map<String, Object>>> list(@PathVariable Long postId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId).orderByAsc(Comment::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("content", c.getContent());
            m.put("parentId", c.getParentId());
            m.put("createTime", c.getCreateTime());
            User u = userMapper.selectById(c.getUserId());
            m.put("userId", c.getUserId());
            m.put("nickname", u != null ? u.getNickname() : "未知");
            m.put("avatar", u != null ? u.getAvatar() : null);
            result.add(m);
        }
        return Result.success(result);
    }

    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        Long postId = Long.valueOf(body.get("postId").toString());
        String content = body.get("content").toString();

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : 0L);
        commentMapper.insert(comment);

        // 通知帖子发布者
        Post post = postMapper.selectById(postId);
        if (post != null && !post.getUserId().equals(userId)) {
            User commenter = userMapper.selectById(userId);
            Message msg = new Message();
            msg.setToUserId(post.getUserId());
            msg.setType("comment");
            msg.setTitle("新评论");
            msg.setContent((commenter != null ? commenter.getNickname() : "用户") + " 评论了你的帖子: " + content);
            msg.setRelatedPostId(postId);
            msg.setIsRead(0);
            messageMapper.insert(msg);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", comment.getId());
        User u = userMapper.selectById(userId);
        r.put("nickname", u != null ? u.getNickname() : "未知");
        r.put("createTime", comment.getCreateTime());
        return Result.success(r);
    }
}
