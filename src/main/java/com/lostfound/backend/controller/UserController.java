package com.lostfound.backend.controller;

import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.vo.UserInfoVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 获取当前登录用户信息
     * GET /api/user/info
     */
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
}