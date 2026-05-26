package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.*;
import com.lostfound.backend.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pm")
@RequiredArgsConstructor
public class PrivateMessageController {
    private final PrivateMessageMapper pmMapper;
    private final UserMapper userMapper;

    /** 会话列表（最近联系人） */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> conversations(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        // 查找所有与我有关的私信
        List<PrivateMessage> all = pmMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w.eq(PrivateMessage::getFromUserId, userId).or().eq(PrivateMessage::getToUserId, userId))
                .orderByDesc(PrivateMessage::getCreateTime));

        // 按对话对象分组
        Map<Long, PrivateMessage> latestMap = new LinkedHashMap<>();
        for (PrivateMessage pm : all) {
            Long otherId = pm.getFromUserId().equals(userId) ? pm.getToUserId() : pm.getFromUserId();
            latestMap.putIfAbsent(otherId, pm);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, PrivateMessage> e : latestMap.entrySet()) {
            User u = userMapper.selectById(e.getKey());
            if (u == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            m.put("lastMsg", e.getValue().getContent());
            m.put("lastTime", e.getValue().getCreateTime());
            // 未读数
            long unread = pmMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getFromUserId, e.getKey())
                    .eq(PrivateMessage::getToUserId, userId)
                    .eq(PrivateMessage::getIsRead, 0));
            m.put("unread", unread);
            result.add(m);
        }
        return Result.success(result);
    }

    /** 与某人的聊天记录 */
    @GetMapping("/with/{otherUserId}")
    public Result<List<PrivateMessage>> chat(@PathVariable Long otherUserId, Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        List<PrivateMessage> msgs = pmMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w.eq(PrivateMessage::getFromUserId, userId).eq(PrivateMessage::getToUserId, otherUserId))
                .or(w -> w.eq(PrivateMessage::getFromUserId, otherUserId).eq(PrivateMessage::getToUserId, userId))
                .orderByAsc(PrivateMessage::getCreateTime));
        // 标记已读
        for (PrivateMessage pm : msgs) {
            if (pm.getToUserId().equals(userId) && pm.getIsRead() == 0) {
                pm.setIsRead(1);
                pmMapper.updateById(pm);
            }
        }
        return Result.success(msgs);
    }

    /** 发送私信 */
    @PostMapping
    public Result<PrivateMessage> send(@RequestBody Map<String, Object> body, Authentication auth) {
        Long fromId = ((User) auth.getPrincipal()).getId();
        PrivateMessage pm = new PrivateMessage();
        pm.setFromUserId(fromId);
        pm.setToUserId(Long.valueOf(body.get("toUserId").toString()));
        pm.setContent(body.get("content").toString());
        pm.setIsRead(0);
        pmMapper.insert(pm);
        return Result.success(pm);
    }
}
