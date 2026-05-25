package com.lostfound.backend.service;

import com.lostfound.backend.entity.Post;

import java.util.List;

public interface MatchingService {

    /** 对一篇新发布的帖子执行匹配 */
    List<MatchResult> match(Post newPost);

    /** 每日补偿匹配：对所有7天内未匹配的进行中帖子 */
    void scheduledMatch();

    record MatchResult(Post matchedPost, int totalScore, int itemScore, int colorScore,
                       int locationScore, int timeScore) {
        public boolean shouldNotify() { return totalScore >= 60; }
    }
}
