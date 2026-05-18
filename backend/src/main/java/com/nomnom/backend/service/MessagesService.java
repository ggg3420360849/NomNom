package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.MessagesEntity;
import com.nomnom.backend.mapper.MessagesMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessagesService {

    private final MessagesMapper messagesMapper;

    public MessagesService(MessagesMapper messagesMapper) {
        this.messagesMapper = messagesMapper;
    }

    public List<MessagesEntity> getUserMessages(Long userId) {
        return messagesMapper.selectList(
                new LambdaQueryWrapper<MessagesEntity>()
                        .eq(MessagesEntity::getUserId, userId.intValue())
                        .orderByDesc(MessagesEntity::getCreateTime));
    }

    public long getUnreadCount(Long userId) {
        return messagesMapper.selectCount(
                new LambdaQueryWrapper<MessagesEntity>()
                        .eq(MessagesEntity::getUserId, userId.intValue())
                        .eq(MessagesEntity::getReadStatus, 0));
    }

    @Transactional
    public void markAsRead(Long id) {
        MessagesEntity entity = messagesMapper.selectById(id);
        if (entity != null) {
            entity.setReadStatus(1);
            messagesMapper.updateById(entity);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<MessagesEntity> list = messagesMapper.selectList(
                new LambdaQueryWrapper<MessagesEntity>()
                        .eq(MessagesEntity::getUserId, userId.intValue())
                        .eq(MessagesEntity::getReadStatus, 0));
        for (MessagesEntity entity : list) {
            entity.setReadStatus(1);
            messagesMapper.updateById(entity);
        }
    }

    @Transactional
    public void deleteMessage(Long id) {
        messagesMapper.deleteById(id);
    }

    @Transactional
    public void createMessage(Long userId, String content, Integer type) {
        MessagesEntity entity = new MessagesEntity();
        entity.setUserId(userId.intValue());
        entity.setContent(content);
        entity.setType(type);
        entity.setReadStatus(0);
        entity.setCreateTime(LocalDateTime.now());
        messagesMapper.insert(entity);
    }
}
