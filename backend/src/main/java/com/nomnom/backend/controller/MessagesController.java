package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.MessagesEntity;
import com.nomnom.backend.service.MessagesService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessagesController {

    private final MessagesService messagesService;

    public MessagesController(MessagesService messagesService) {
        this.messagesService = messagesService;
    }

    @GetMapping
    public ApiResponse<List<MessagesEntity>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(messagesService.getUserMessages(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.ok(messagesService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messagesService.markAsRead(userId, id);
        return ApiResponse.ok("ok", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messagesService.markAllAsRead(userId);
        return ApiResponse.ok("ok", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        messagesService.deleteMessage(id);
        return ApiResponse.ok("已删除", null);
    }
}
