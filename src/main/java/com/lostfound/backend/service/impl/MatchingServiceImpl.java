package com.lostfound.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import com.lostfound.backend.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final PostMapper postMapper;
    private final MatchRecordMapper matchRecordMapper;
    private final MessageMapper messageMapper;

    private static final int MATCH_DAYS = 7;
    private static final int THRESHOLD = 60;

    @Override
    @Transactional
    public List<MatchResult> match(Post newPost) {
        List<MatchResult> results = new ArrayList<>();

        // 查找7天内反向类型、进行中的帖子
        int oppositeType = newPost.getType() == 0 ? 1 : 0;
        LocalDate cutoff = LocalDate.now().minusDays(MATCH_DAYS);

        List<Post> candidates = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getType, oppositeType)
                .eq(Post::getStatus, 0)
                .ge(Post::getLostTime, cutoff)
                .ne(Post::getUserId, newPost.getUserId()));

        for (Post candidate : candidates) {
            MatchResult result = computeScore(newPost, candidate);
            if (result.shouldNotify()) {
                // 去重检查
                Long count = matchRecordMapper.selectCount(new LambdaQueryWrapper<MatchRecord>()
                        .and(w -> w
                                .eq(MatchRecord::getLostPostId, newPost.getType() == 0 ? newPost.getId() : candidate.getId())
                                .eq(MatchRecord::getFoundPostId, newPost.getType() == 1 ? newPost.getId() : candidate.getId())
                        ));
                if (count > 0) continue;

                // 保存匹配记录
                MatchRecord record = new MatchRecord();
                record.setLostPostId(newPost.getType() == 0 ? newPost.getId() : candidate.getId());
                record.setFoundPostId(newPost.getType() == 1 ? newPost.getId() : candidate.getId());
                record.setScore(result.totalScore());
                record.setItemScore(result.itemScore());
                record.setColorScore(result.colorScore());
                record.setLocationScore(result.locationScore());
                record.setTimeScore(result.timeScore());
                matchRecordMapper.insert(record);

                // 更新双方帖子状态为已匹配
                updatePostStatus(newPost.getId());
                updatePostStatus(candidate.getId());

                // 发送通知给双方
                sendMatchNotification(newPost.getUserId(), result, newPost, candidate);
                sendMatchNotification(candidate.getUserId(), result, candidate, newPost);

                results.add(result);
            }
        }
        return results;
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    @Transactional
    public void scheduledMatch() {
        log.info("开始每日补偿匹配...");
        LocalDate cutoff = LocalDate.now().minusDays(MATCH_DAYS);
        List<Post> activePosts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 0)
                .ge(Post::getLostTime, cutoff));
        log.info("待匹配帖子数: {}", activePosts.size());

        int matchCount = 0;
        for (Post post : activePosts) {
            List<MatchResult> results = match(post);
            matchCount += results.size();
        }
        log.info("补偿匹配完成，产生 {} 条新匹配", matchCount);
    }

    private MatchResult computeScore(Post p1, Post p2) {
        int itemScore = 0, colorScore = 0, locationScore = 0, timeScore = 0;

        // 物品大类：完全一致40分，否则0（一票否决）
        if (p1.getItemCategory() != null && p1.getItemCategory().equals(p2.getItemCategory())) {
            itemScore = 40;
        } else {
            return new MatchResult(null, 0, 0, 0, 0, 0); // 大类不匹配，直接返回0
        }

        // 颜色：完全一致30分
        if (p1.getColor() != null && p1.getColor().equals(p2.getColor())) {
            colorScore = 30;
        }

        // 地点：三级全一致20分，两级10分，一级5分
        boolean campusMatch = p1.getLocationCampus() != null && p1.getLocationCampus().equals(p2.getLocationCampus());
        boolean areaMatch = p1.getLocationArea() != null && p1.getLocationArea().equals(p2.getLocationArea());
        boolean detailMatch = p1.getLocationDetail() != null && p1.getLocationDetail().equals(p2.getLocationDetail());

        if (campusMatch && areaMatch && detailMatch) locationScore = 20;
        else if (campusMatch && areaMatch) locationScore = 10;
        else if (campusMatch) locationScore = 5;

        // 时间差：≤1天10分，1-3天5分，>3天0分
        if (p1.getLostTime() != null && p2.getLostTime() != null) {
            long daysDiff = Math.abs(ChronoUnit.DAYS.between(p1.getLostTime(), p2.getLostTime()));
            if (daysDiff <= 1) timeScore = 10;
            else if (daysDiff <= 3) timeScore = 5;
        }

        int total = itemScore + colorScore + locationScore + timeScore;
        return new MatchResult(p2, total, itemScore, colorScore, locationScore, timeScore);
    }

    private void updatePostStatus(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null && post.getStatus() == 0) {
            post.setStatus(1); // 已匹配
            postMapper.updateById(post);
        }
    }

    private void sendMatchNotification(Long toUserId, MatchResult result, Post myPost, Post otherPost) {
        String title = String.format("物品匹配成功！匹配度 %d 分", result.totalScore());
        String content = String.format(
                "您的「%s」与一条「%s」匹配成功\n匹配详情：大类 +%d | 颜色 +%d | 地点 +%d | 时间 +%d",
                myPost.getTitle() != null ? myPost.getTitle() : "无标题帖子",
                otherPost.getTitle() != null ? otherPost.getTitle() : "无标题帖子",
                result.itemScore(), result.colorScore(), result.locationScore(), result.timeScore()
        );
        Message msg = new Message();
        msg.setToUserId(toUserId);
        msg.setType("match");
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedPostId(otherPost.getId());
        msg.setIsRead(0);
        messageMapper.insert(msg);
    }
}
