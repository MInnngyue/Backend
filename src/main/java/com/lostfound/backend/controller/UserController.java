package com.lostfound.backend.controller;

import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.Post;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.PostMapper;
import com.lostfound.backend.mapper.UserMapper;
import com.lostfound.backend.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @GetMapping("/info")
    public Result<UserInfoVO> info(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        UserInfoVO vo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .creditScore(user.getCreditScore())
                .successCount(user.getSuccessCount())
                .signature(user.getSignature())
                .build();

        return Result.success(vo);
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@RequestBody Map<String, Object> body, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        User dbUser = userMapper.selectById(user.getId());
        
        if (body.containsKey("nickname")) dbUser.setNickname((String) body.get("nickname"));
        if (body.containsKey("signature")) dbUser.setSignature((String) body.get("signature"));
        if (body.containsKey("phone")) dbUser.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) dbUser.setEmail((String) body.get("email"));
        if (body.containsKey("avatar")) dbUser.setAvatar((String) body.get("avatar"));
        
        userMapper.updateById(dbUser);
        return info(authentication);
    }

    @GetMapping("/posts")
    public Result<List<Post>> myPosts(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Post> posts = postMapper.selectList(
            new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, user.getId())
                .orderByDesc(Post::getCreateTime)
                .last("LIMIT " + ((page - 1) * size) + "," + size)
        );
        return Result.success(posts);
    }
}