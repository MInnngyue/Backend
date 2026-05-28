package com.lostfound.backend.controller;

import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.Claim;
import com.lostfound.backend.entity.Post;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.ClaimMapper;
import com.lostfound.backend.mapper.PostMapper;
import com.lostfound.backend.mapper.UserMapper;
import com.lostfound.backend.service.ClaimService;
import com.lostfound.backend.vo.ClaimVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimMapper claimMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @PostMapping
    public Result<Claim> create(Authentication auth,
                                @RequestParam Long postId,
                                @RequestParam(required = false) Long matchId) {
        Long userId = ((User) auth.getPrincipal()).getId();
        return Result.success(claimService.createClaim(userId, postId, matchId));
    }

    @PutMapping("/{id}/confirm")
    public Result<Claim> confirm(@PathVariable Long id, Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        return Result.success(claimService.confirm(id, userId));
    }

    @PutMapping("/{id}/cancel")
    public Result<Claim> cancel(@PathVariable Long id, Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        return Result.success(claimService.cancelClaim(id, userId));
    }

    @GetMapping("/post/{postId}")
    public Result<List<Claim>> byPost(@PathVariable Long postId) {
        return Result.success(claimService.getByPostId(postId));
    }

    // 我的认领记录
    @GetMapping("/my")
    public Result<List<ClaimVO>> myClaims(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        List<Claim> claims = claimMapper.selectList(
            new LambdaQueryWrapper<Claim>()
                .and(w -> w.eq(Claim::getClaimUserId, userId).or().eq(Claim::getPostOwnerId, userId))
                .orderByDesc(Claim::getCreateTime)
        );

        List<ClaimVO> vos = new ArrayList<>();
        for (Claim c : claims) {
            Post post = postMapper.selectById(c.getPostId());
            if (post == null) continue;

            String myRole = userId.equals(c.getClaimUserId()) ? "claimer" : "owner";
            Long otherId = myRole.equals("claimer") ? c.getPostOwnerId() : c.getClaimUserId();
            User other = userMapper.selectById(otherId);
            String otherName = other != null ? (other.getNickname() != null ? other.getNickname() : other.getUsername()) : "未知用户";

            ClaimVO vo = ClaimVO.builder()
                .claimId(c.getId())
                .postId(c.getPostId())
                .postTitle(post.getTitle())
                .postType(post.getType())
                .postStatus(post.getStatus())
                .otherPartyName(otherName)
                .otherPartyId(otherId)
                .myRole(myRole)
                .claimStatus(c.getStatus())
                .ownerConfirmed(c.getOwnerConfirmed())
                .claimerConfirmed(c.getClaimerConfirmed())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
            vos.add(vo);
        }
        return Result.success(vos);
    }
}
