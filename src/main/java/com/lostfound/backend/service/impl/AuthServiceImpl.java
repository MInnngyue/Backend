package com.lostfound.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.dto.LoginDTO;
import com.lostfound.backend.dto.RegisterDTO;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.UserMapper;
import com.lostfound.backend.service.AuthService;
import com.lostfound.backend.utils.JwtUtil;
import com.lostfound.backend.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );

        // 2. 用户不存在 → 模糊提示，防止枚举攻击
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. ✅ 检查账号是否被禁用
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 5. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("==> 用户登录成功：{}", user.getUsername());

        // 6. 返回登录信息
        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .token(token)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)  // ✅ 加事务
    public void register(RegisterDTO dto) {
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. ✅ 检查邮箱是否已注册（如果DTO有email字段）
        if (dto.getEmail() != null) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, dto.getEmail())
            );
            if (emailCount > 0) {
                throw new BusinessException(400, "邮箱已被注册");
            }
        }

        // 3. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setStatus(0);
        user.setCreditScore(100);
        user.setSuccessCount(0);

        userMapper.insert(user);
        log.info("==> 新用户注册成功：{}", dto.getUsername());
    }
}