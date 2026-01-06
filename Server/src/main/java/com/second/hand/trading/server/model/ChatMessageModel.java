package com.second.hand.trading.server.model;

import java.io.Serializable;
import java.util.Date;

/**
 * sh_chat_message
 */
public class ChatMessageModel implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 所属会话ID
     */
    private Long conversationId;

    /**
     * 发送者用户ID
     */
    private Long senderId;

    /**
     * 接收者用户ID
     */
    private Long receiverId;

    /**
     * 关联的闲置物ID，可为空
     */
    private Long idleId;

    /**
     * 消息类型（0文本、1图片、2系统）
     */
    private Integer messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附加数据(JSON字符串)
     */
    private String extraPayload;

    /**
     * 发送时间
     */
    private Date sendTime;

    /**
     * 阅读时间
     */
    private Date readTime;

    private UserModel sender;

    private UserModel receiver;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

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

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }

    public UserModel getSender() {
        return sender;
    }

    public void setSender(UserModel sender) {
        this.sender = sender;
    }

    public UserModel getReceiver() {
        return receiver;
    }

    public void setReceiver(UserModel receiver) {
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ChatMessageModel other = (ChatMessageModel) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getConversationId() == null ? other.getConversationId() == null : this.getConversationId().equals(other.getConversationId()))
            && (this.getSenderId() == null ? other.getSenderId() == null : this.getSenderId().equals(other.getSenderId()))
            && (this.getReceiverId() == null ? other.getReceiverId() == null : this.getReceiverId().equals(other.getReceiverId()))
            && (this.getIdleId() == null ? other.getIdleId() == null : this.getIdleId().equals(other.getIdleId()))
            && (this.getMessageType() == null ? other.getMessageType() == null : this.getMessageType().equals(other.getMessageType()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getExtraPayload() == null ? other.getExtraPayload() == null : this.getExtraPayload().equals(other.getExtraPayload()))
            && (this.getSendTime() == null ? other.getSendTime() == null : this.getSendTime().equals(other.getSendTime()))
            && (this.getReadTime() == null ? other.getReadTime() == null : this.getReadTime().equals(other.getReadTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getConversationId() == null) ? 0 : getConversationId().hashCode());
        result = prime * result + ((getSenderId() == null) ? 0 : getSenderId().hashCode());
        result = prime * result + ((getReceiverId() == null) ? 0 : getReceiverId().hashCode());
        result = prime * result + ((getIdleId() == null) ? 0 : getIdleId().hashCode());
        result = prime * result + ((getMessageType() == null) ? 0 : getMessageType().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getExtraPayload() == null) ? 0 : getExtraPayload().hashCode());
        result = prime * result + ((getSendTime() == null) ? 0 : getSendTime().hashCode());
        result = prime * result + ((getReadTime() == null) ? 0 : getReadTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", conversationId=").append(conversationId);
        sb.append(", senderId=").append(senderId);
        sb.append(", receiverId=").append(receiverId);
        sb.append(", idleId=").append(idleId);
        sb.append(", messageType=").append(messageType);
        sb.append(", content=").append(content);
        sb.append(", extraPayload=").append(extraPayload);
        sb.append(", sendTime=").append(sendTime);
        sb.append(", readTime=").append(readTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
