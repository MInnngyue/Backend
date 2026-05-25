package com.lostfound.backend.controller;

import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.Claim;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

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

    @GetMapping("/post/{postId}")
    public Result<List<Claim>> byPost(@PathVariable Long postId) {
        return Result.success(claimService.getByPostId(postId));
    }
}
