package com.second.hand.trading.server.model;

import java.io.Serializable;
import java.util.Date;

/**
 * sh_chat_conversation
 */
public class ChatConversationModel implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 会话参与人A（较小的用户ID）
     */
    private Long userAId;

    /**
     * 会话参与人B（较大的用户ID）
     */
    private Long userBId;

    /**
     * 关联的闲置物ID，可为空
     */
    private Long idleId;

    /**
     * 关联闲置信息快照，JSON字符串
     */
    private String contextSnapshot;

    /**
     * 最新消息摘要
     */
    private String lastMessagePreview;

    /**
     * 最新消息时间
     */
    private Date lastMessageTime;

    /**
     * 用户A未读数
     */
    private Integer userAUnread;

    /**
     * 用户B未读数
     */
    private Integer userBUnread;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private UserModel userA;

    private UserModel userB;

    private IdleItemModel idle;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAId() {
        return userAId;
    }

    public void setUserAId(Long userAId) {
        this.userAId = userAId;
    }

    public Long getUserBId() {
        return userBId;
    }

    public void setUserBId(Long userBId) {
        this.userBId = userBId;
    }

    public Long getIdleId() {
        return idleId;
    }

    public void setIdleId(Long idleId) {
        this.idleId = idleId;
    }

    public String getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(String contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUserAUnread() {
        return userAUnread;
    }

    public void setUserAUnread(Integer userAUnread) {
        this.userAUnread = userAUnread;
    }

    public Integer getUserBUnread() {
        return userBUnread;
    }

    public void setUserBUnread(Integer userBUnread) {
        this.userBUnread = userBUnread;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public UserModel getUserA() {
        return userA;
    }

    public void setUserA(UserModel userA) {
        this.userA = userA;
    }

    public UserModel getUserB() {
        return userB;
    }

    public void setUserB(UserModel userB) {
        this.userB = userB;
    }

    public IdleItemModel getIdle() {
        return idle;
    }

    public void setIdle(IdleItemModel idle) {
        this.idle = idle;
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
        ChatConversationModel other = (ChatConversationModel) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserAId() == null ? other.getUserAId() == null : this.getUserAId().equals(other.getUserAId()))
            && (this.getUserBId() == null ? other.getUserBId() == null : this.getUserBId().equals(other.getUserBId()))
            && (this.getIdleId() == null ? other.getIdleId() == null : this.getIdleId().equals(other.getIdleId()))
            && (this.getContextSnapshot() == null ? other.getContextSnapshot() == null : this.getContextSnapshot().equals(other.getContextSnapshot()))
            && (this.getLastMessagePreview() == null ? other.getLastMessagePreview() == null : this.getLastMessagePreview().equals(other.getLastMessagePreview()))
            && (this.getLastMessageTime() == null ? other.getLastMessageTime() == null : this.getLastMessageTime().equals(other.getLastMessageTime()))
            && (this.getUserAUnread() == null ? other.getUserAUnread() == null : this.getUserAUnread().equals(other.getUserAUnread()))
            && (this.getUserBUnread() == null ? other.getUserBUnread() == null : this.getUserBUnread().equals(other.getUserBUnread()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserAId() == null) ? 0 : getUserAId().hashCode());
        result = prime * result + ((getUserBId() == null) ? 0 : getUserBId().hashCode());
        result = prime * result + ((getIdleId() == null) ? 0 : getIdleId().hashCode());
        result = prime * result + ((getContextSnapshot() == null) ? 0 : getContextSnapshot().hashCode());
        result = prime * result + ((getLastMessagePreview() == null) ? 0 : getLastMessagePreview().hashCode());
        result = prime * result + ((getLastMessageTime() == null) ? 0 : getLastMessageTime().hashCode());
        result = prime * result + ((getUserAUnread() == null) ? 0 : getUserAUnread().hashCode());
        result = prime * result + ((getUserBUnread() == null) ? 0 : getUserBUnread().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", userAId=").append(userAId);
        sb.append(", userBId=").append(userBId);
        sb.append(", idleId=").append(idleId);
        sb.append(", contextSnapshot=").append(contextSnapshot);
        sb.append(", lastMessagePreview=").append(lastMessagePreview);
        sb.append(", lastMessageTime=").append(lastMessageTime);
        sb.append(", userAUnread=").append(userAUnread);
        sb.append(", userBUnread=").append(userBUnread);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
