package com.second.hand.trading.server.service;

import com.second.hand.trading.server.model.ChatConversationModel;
import com.second.hand.trading.server.model.ChatMessageModel;

import java.util.List;

public interface ChatService {

    ChatMessageModel sendMessage(Long senderId,
                                 Long receiverId,
                                 Long idleId,
                                 Integer messageType,
                                 String content,
                                 String extraPayload);

    List<ChatConversationModel> listConversations(Long userId);

    List<ChatMessageModel> listMessages(Long userId,
                                        Long conversationId,
                                        Long anchorId,
                                        Integer pageSize);

    void resetUnread(Long userId, Long conversationId);
}
