package com.lostfound.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lostfound.backend.common.result.Result;
import com.lostfound.backend.entity.Message;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageMapper messageMapper;

    @GetMapping
    public Result<List<Message>> list(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .orderByDesc(Message::getCreateTime));
        return Result.success(messages);
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0));
        return Result.success(count);
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Message msg = messageMapper.selectById(id);
        if (msg != null) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
        return Result.success(null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        List<Message> unread = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0));
        for (Message msg : unread) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
        return Result.success(null);
    }
}
