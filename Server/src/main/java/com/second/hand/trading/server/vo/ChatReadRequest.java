package com.second.hand.trading.server.vo;

import jakarta.validation.constraints.NotNull;

public class ChatReadRequest {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}
