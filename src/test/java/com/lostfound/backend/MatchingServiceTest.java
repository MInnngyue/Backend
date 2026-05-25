package com.lostfound.backend;

import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import com.lostfound.backend.service.impl.MatchingServiceImpl;
import com.lostfound.backend.service.MatchingService.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private PostMapper postMapper;
    @Mock private MatchRecordMapper matchRecordMapper;
    @Mock private MessageMapper messageMapper;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    // 不使用 real match/matchRecord/Message mapper
    // 仅测试 computeScore 逻辑

    private Post createPost(Integer type, String category, String color,
                            String campus, String area, String detail, LocalDate time) {
        Post p = new Post();
        p.setId(1L);
        p.setUserId(100L);
        p.setType(type);
        p.setItemCategory(category);
        p.setColor(color);
        p.setLocationCampus(campus);
        p.setLocationArea(area);
        p.setLocationDetail(detail);
        p.setLostTime(time);
        p.setTitle("测试");
        p.setStatus(0);
        return p;
    }

    @Test
    void testExactMatch() {
        Post lost = createPost(0, "校园卡", "白色", "主校区", "食堂", "一食堂", LocalDate.of(2026, 5, 25));
        Post found = createPost(1, "校园卡", "白色", "主校区", "食堂", "一食堂", LocalDate.of(2026, 5, 25));

        when(postMapper.selectList(any())).thenReturn(List.of(found));
        when(matchRecordMapper.selectCount(any())).thenReturn(0L);
        doReturn(1).when(matchRecordMapper).insert(any(MatchRecord.class));
        when(postMapper.selectById(any())).thenReturn(lost);
        doReturn(1).when(messageMapper).insert(any(Message.class));

        List<MatchResult> results = matchingService.match(lost);
        assertEquals(1, results.size());
        MatchResult r = results.get(0);
        assertTrue(r.totalScore() >= 85); // 40+30+20+10=100（理想全匹配）
    }

    @Test
    void testNoMatchDifferentCategory() {
        Post lost = createPost(0, "校园卡", "白色", "主校区", null, null, LocalDate.of(2026, 5, 25));
        Post found = createPost(1, "电子产品", "白色", "主校区", null, null, LocalDate.of(2026, 5, 25));

        when(postMapper.selectList(any())).thenReturn(List.of(found));

        List<MatchResult> results = matchingService.match(lost);
        assertTrue(results.isEmpty()); // 大类不匹配
    }

    @Test
    void testBelowThresholdNoNotification() {
        // 仅大类+校区匹配(40+5=45)，低于60分阈值，不应推送
        Post lost = createPost(0, "校园卡", "白色", "主校区", null, null, LocalDate.of(2026, 5, 25));
        Post found = createPost(1, "校园卡", "黑色", "主校区", null, null, LocalDate.of(2026, 5, 20));

        when(postMapper.selectList(any())).thenReturn(List.of(found));

        List<MatchResult> results = matchingService.match(lost);
        assertTrue(results.isEmpty(), "低于60分阈值不应推送通知");
    }
}
