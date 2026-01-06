package com.second.hand.trading.server.controller;

import com.alibaba.fastjson2.JSON;
import com.second.hand.trading.server.model.ChatConversationModel;
import com.second.hand.trading.server.model.ChatMessageModel;
import com.second.hand.trading.server.service.ChatService;
import com.second.hand.trading.server.vo.ChatReadRequest;
import com.second.hand.trading.server.vo.ChatSendRequest;
import com.second.hand.trading.server.vo.ResultVo;
import com.second.hand.trading.server.websocket.ChatSessionRegistry;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ChatSessionRegistry chatSessionRegistry;

    @GetMapping("/conversations")
    public ResultVo<List<ChatConversationModel>> listConversations(@CookieValue("shUserId")
                                                                   @NotNull(message = "登录异常 请重新登录")
                                                                   @NotEmpty(message = "登录异常 请重新登录") String shUserId) {
        Long userId = Long.valueOf(shUserId);
        return ResultVo.success(chatService.listConversations(userId));
    }

    @GetMapping("/messages")
    public ResultVo<List<ChatMessageModel>> listMessages(@CookieValue("shUserId")
                                                         @NotNull(message = "登录异常 请重新登录")
                                                         @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                                         @RequestParam Long conversationId,
                                                         @RequestParam(required = false) Long anchorId,
                                                         @RequestParam(required = false) Integer pageSize) {
        Long userId = Long.valueOf(shUserId);
        return ResultVo.success(chatService.listMessages(userId, conversationId, anchorId, pageSize));
    }

    @PostMapping("/send")
    public ResultVo<ChatMessageModel> sendMessage(@CookieValue("shUserId")
                                                  @NotNull(message = "登录异常 请重新登录")
                                                  @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                                  @RequestBody @Valid ChatSendRequest request) {
        Long senderId = Long.valueOf(shUserId);
        ChatMessageModel message = chatService.sendMessage(senderId,
                request.getReceiverId(),
                request.getIdleId(),
                request.getMessageType(),
                request.getContent(),
                request.getExtraPayload());
        broadcastMessage(senderId, request.getReceiverId(), message);
        return ResultVo.success(message);
    }

    @PostMapping("/read")
    public ResultVo<Void> readConversation(@CookieValue("shUserId")
                                           @NotNull(message = "登录异常 请重新登录")
                                           @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                           @RequestBody @Valid ChatReadRequest request) {
        Long userId = Long.valueOf(shUserId);
        chatService.resetUnread(userId, request.getConversationId());
        return ResultVo.success();
    }

    private void broadcastMessage(Long senderId, Long receiverId, ChatMessageModel message) {
        Map<String, Object> payload = new HashMap<>(4);
        payload.put("event", "chat_message");
        payload.put("message", message);
        payload.put("conversationId", message.getConversationId());
        String serialized = JSON.toJSONString(payload);
        chatSessionRegistry.broadcastToUsers(Arrays.asList(senderId, receiverId), serialized);
    }
}
