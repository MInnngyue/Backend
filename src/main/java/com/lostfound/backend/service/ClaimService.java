package com.lostfound.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.entity.Claim;
import com.lostfound.backend.entity.Message;
import com.lostfound.backend.entity.Post;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.ClaimMapper;
import com.lostfound.backend.mapper.MessageMapper;
import com.lostfound.backend.mapper.PostMapper;
import com.lostfound.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimMapper claimMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final CreditScoreService creditScoreService;

    /** 发起认领 */
    @Transactional
    public Claim createClaim(Long claimUserId, Long postId, Long matchId) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        if (post.getUserId().equals(claimUserId))
            throw new BusinessException(400, "不能认领自己的帖子");
        if (post.getStatus() != 1 && post.getStatus() != 0)
            throw new BusinessException(400, "帖子状态不允许认领");

        User claimer = userMapper.selectById(claimUserId);
        if (!creditScoreService.canClaim(claimer))
            throw new BusinessException(403, "信用分不足80，无法发起认领");

        // 检查是否已有进行中的认领
        Long existing = claimMapper.selectCount(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getPostId, postId)
                .eq(Claim::getClaimUserId, claimUserId)
                .in(Claim::getStatus, 0, 1));
        if (existing > 0)
            throw new BusinessException(400, "你已对此帖子发起过认领");

        Claim claim = new Claim();
        claim.setPostId(postId);
        claim.setClaimUserId(claimUserId);
        claim.setPostOwnerId(post.getUserId());
        claim.setMatchId(matchId);
        claim.setStatus(0);
        claim.setOwnerConfirmed(0);
        claim.setClaimerConfirmed(0);
        claimMapper.insert(claim);

        // 通知发布者
        String claimerName = claimer.getNickname() != null ? claimer.getNickname() : claimer.getUsername();
        sendNotification(post.getUserId(), "有人认领了你的帖子",
            claimerName + " 对你的帖子「" + post.getTitle() + "」发起了认领申请，请前往认领进度页面确认",
            postId);

        // 更新帖子状态为认领中
        if (post.getStatus() == 1) {
            post.setStatus(2);
            postMapper.updateById(post);
        }

        return claim;
    }

    /** 确认认领（发布者或认领者点击确认） */
    @Transactional
    public Claim confirm(Long claimId, Long userId) {
        Claim claim = claimMapper.selectById(claimId);
        if (claim == null) throw new BusinessException(404, "认领记录不存在");
        if (claim.getStatus() != 0 && claim.getStatus() != 1)
            throw new BusinessException(400, "当前状态不可确认");

        if (userId.equals(claim.getPostOwnerId())) {
            claim.setOwnerConfirmed(1);
            // 通知认领者：发布者已确认
            Post p = postMapper.selectById(claim.getPostId());
            sendNotification(claim.getClaimUserId(), "认领进度更新",
                "发布者已确认你的认领，请尽快确认以完成认领流程",
                claim.getPostId());
        } else if (userId.equals(claim.getClaimUserId())) {
            claim.setClaimerConfirmed(1);
            // 通知发布者：认领者已确认
            Post p = postMapper.selectById(claim.getPostId());
            sendNotification(claim.getPostOwnerId(), "认领进度更新",
                "认领者已确认物品归属，请尽快确认以完成认领流程",
                claim.getPostId());
        } else {
            throw new BusinessException(403, "无权操作");
        }

        // 双方都已确认 → 完结
        if (claim.getOwnerConfirmed() == 1 && claim.getClaimerConfirmed() == 1) {
            claim.setStatus(2); // 已完结
            // 更新帖子状态
            Post post = postMapper.selectById(claim.getPostId());
            if (post != null) {
                post.setStatus(3); // 已完结
                postMapper.updateById(post);
            }
            // 双方加分
            creditScoreService.addScore(claim.getPostOwnerId(),
                    CreditScoreService.SCORE_SUCCESS_CLAIM);
            creditScoreService.addScore(claim.getClaimUserId(),
                    CreditScoreService.SCORE_SUCCESS_CLAIM);
            // 更新成功认领次数
            incrementSuccessCount(claim.getPostOwnerId());
            incrementSuccessCount(claim.getClaimUserId());
            // 通知双方认领完成
            sendNotification(claim.getPostOwnerId(), "认领已完成",
                "恭喜！认领流程已完结，双方信用分各+5", claim.getPostId());
            sendNotification(claim.getClaimUserId(), "认领已完成",
                "恭喜！认领流程已完结，双方信用分各+5", claim.getPostId());
        } else {
            claim.setStatus(1); // 双方已确认（部分）
        }

        claimMapper.updateById(claim);
        return claim;
    }

    /** 获取帖子的认领记录 */
    public List<Claim> getByPostId(Long postId) {
        return claimMapper.selectList(new LambdaQueryWrapper<Claim>()
                .eq(Claim::getPostId, postId)
                .orderByDesc(Claim::getCreateTime));
    }

    private void incrementSuccessCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setSuccessCount(user.getSuccessCount() + 1);
            userMapper.updateById(user);
        }
    }

    private void sendNotification(Long toUserId, String title, String content, Long relatedPostId) {
        Message msg = new Message();
        msg.setFromUserId(0L); // 系统通知
        msg.setToUserId(toUserId);
        msg.setType("claim");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedPostId(relatedPostId);
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }
}
