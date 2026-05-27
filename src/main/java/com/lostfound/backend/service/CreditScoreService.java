package com.lostfound.backend.service;

import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreditScoreService {

    private final UserMapper userMapper;

    public static final int SCORE_SUCCESS_CLAIM = 2;
    public static final int SCORE_GOOD_STANDING = 5;
    public static final int SCORE_FRAUD = -20;
    public static final int SCORE_VIOLATION = -10;

    public static final int TIER_EXEMPT = 100;
    public static final int TIER_NORMAL = 80;
    public static final int TIER_RESTRICTED = 60;
    public static final int TIER_BANNED = 0;

    @Transactional
    public void addScore(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int newScore = Math.max(0, Math.min(100, user.getCreditScore() + delta));
            user.setCreditScore(newScore);
            userMapper.updateById(user);
        }
    }

    public boolean canPublish(User user) {
        return user.getCreditScore() >= TIER_RESTRICTED;
    }

    public boolean canClaim(User user) {
        return user.getCreditScore() >= TIER_NORMAL;
    }

    public int getTier(User user) {
        int score = user.getCreditScore();
        if (score >= TIER_EXEMPT) return 4;
        if (score >= TIER_NORMAL) return 3;
        if (score >= TIER_RESTRICTED) return 2;
        if (score > TIER_BANNED) return 1;
        return 0;
    }

    public String getTierName(User user) {
        return switch (getTier(user)) {
            case 4 -> "信用优秀";
            case 3 -> "信用良好";
            case 2 -> "信用受限";
            case 1 -> "信用警告";
            default -> "已封禁";
        };
    }
}
