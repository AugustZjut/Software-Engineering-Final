package com.second.hand.trading.server.vo;

import jakarta.validation.constraints.NotNull;

public class ChatSendRequest {

    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    private Long idleId;

    private Integer messageType;

    private String content;

    private String extraPayload;

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getIdleId() {
        return idleId;
    }

    public void setIdleId(Long idleId) {
        this.idleId = idleId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtraPayload() {
        return extraPayload;
    }

    public void setExtraPayload(String extraPayload) {
        this.extraPayload = extraPayload;
    }
}
