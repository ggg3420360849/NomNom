package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nomnom.backend.entity.MessagesEntity;
import com.nomnom.backend.entity.UserEntity;
import com.nomnom.backend.mapper.MessagesMapper;
import com.nomnom.backend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessagesService {

    private final MessagesMapper messagesMapper;
    private final UserMapper userMapper;

    public MessagesService(MessagesMapper messagesMapper, UserMapper userMapper) {
        this.messagesMapper = messagesMapper;
        this.userMapper = userMapper;
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
    public void markAsRead(Long userId, Long id) {
        if (userId == null || id == null) {
            return;
        }
        MessagesEntity entity = new MessagesEntity();
        entity.setReadStatus(1);
        messagesMapper.update(entity,
                new LambdaUpdateWrapper<MessagesEntity>()
                        .eq(MessagesEntity::getId, id.intValue())
                        .eq(MessagesEntity::getUserId, userId.intValue())
                        .eq(MessagesEntity::getReadStatus, 0));
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        if (userId == null) {
            return;
        }
        MessagesEntity entity = new MessagesEntity();
        entity.setReadStatus(1);
        messagesMapper.update(entity,
                new LambdaUpdateWrapper<MessagesEntity>()
                        .eq(MessagesEntity::getUserId, userId.intValue())
                        .eq(MessagesEntity::getReadStatus, 0));
    }

    @Transactional
    public void deleteMessage(Long id) {
        messagesMapper.deleteById(id);
    }

    @Transactional
    public void createMessage(Long userId, String content, Integer type) {
        if (userId == null || content == null || content.trim().isEmpty()) {
            return;
        }
        MessagesEntity entity = new MessagesEntity();
        entity.setUserId(userId.intValue());
        entity.setContent(truncate(content.trim(), 255));
        entity.setType(type);
        entity.setReadStatus(0);
        entity.setCreateTime(LocalDateTime.now());
        messagesMapper.insert(entity);
    }

    @Transactional
    public void createMessageForRole(Integer role, String content, Integer type) {
        if (role == null || content == null || content.trim().isEmpty()) {
            return;
        }
        List<UserEntity> users = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getRole, role));
        for (UserEntity user : users) {
            if (user.getId() != null) {
                createMessage(user.getId(), content, type);
            }
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }
}
