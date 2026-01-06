<template>
    <div class="chat-page-wrapper">
        <app-head></app-head>
        <app-body>
            <div class="chat-layout">
                <div class="chat-sidebar">
                    <div class="chat-sidebar-header">
                        <div class="chat-sidebar-title">会话列表</div>
                        <div class="chat-sidebar-unread" v-if="unreadTotal">未读 {{ unreadTotal }}</div>
                    </div>
                    <el-scrollbar class="chat-conversation-scroll">
                        <div v-for="conversation in conversations"
                             :key="conversation.id"
                             class="chat-conversation-item"
                             :class="{active: conversation.id === activeConversationId}"
                             @click="selectConversation(conversation)">
                            <div class="chat-conversation-name">{{ resolvePeerName(conversation) }}</div>
                            <div class="chat-conversation-preview">{{ formatPreview(conversation) }}</div>
                            <div class="chat-conversation-meta">
                                <span class="chat-conversation-time">{{ formatTimestamp(conversation.lastMessageTime) }}</span>
                                <el-badge v-if="conversationUnread(conversation)"
                                          :value="conversationUnread(conversation)"
                                          class="chat-conversation-badge"></el-badge>
                            </div>
                        </div>
                        <div v-if="!conversations.length" class="chat-conversation-empty">暂无会话</div>
                    </el-scrollbar>
                </div>
                <div class="chat-content">
                    <template v-if="activeConversation">
                        <div class="chat-content-header">
                            <div class="chat-peer-name">{{ resolvePeerName(activeConversation) }}</div>
                            <div class="chat-peer-desc" v-if="activeConversation.idle">来自闲置：{{ activeConversation.idle.idleName }}</div>
                        </div>
                        <chat-context-bar
                            :idle="activeConversation.idle"
                            @view-detail="viewIdleDetail">
                        </chat-context-bar>
                        <chat-message-list
                            :messages="messages"
                            :current-user-id="currentUserId"
                            :loading="loadingMessages"
                            :can-load-more="canLoadMore"
                            @load-more="loadMoreMessages">
                        </chat-message-list>
                        <chat-input
                            v-model="messageDraft"
                            :sending="sendingMessage"
                            @send="handleSend">
                        </chat-input>
                    </template>
                    <template v-else>
                        <div class="chat-empty-state">
                            <div class="chat-empty-title">{{ pendingTarget ? '准备发起新会话' : '请选择一个会话' }}</div>
                            <div class="chat-empty-subtitle">从列表中选择已有会话，或在商品详情页点击“联系卖家”开始聊天。</div>
                            <chat-input
                                v-if="pendingTarget"
                                v-model="messageDraft"
                                :sending="sendingMessage"
                                @send="handleSend">
                            </chat-input>
                        </div>
                    </template>
                </div>
            </div>
            <app-foot></app-foot>
        </app-body>
    </div>
</template>

<script>
import AppHead from '../common/AppHeader.vue';
import AppBody from '../common/AppPageBody.vue';
import AppFoot from '../common/AppFoot.vue';
import ChatMessageList from '../common/ChatMessageList.vue';
import ChatInput from '../common/ChatInput.vue';
import ChatContextBar from '../common/ChatContextBar.vue';

export default {
    name: 'chat',
    components: {
        AppHead,
        AppBody,
        AppFoot,
        ChatMessageList,
        ChatInput,
        ChatContextBar
    },
    data() {
        return {
            loadingMessages: false,
            sendingMessage: false,
            messageDraft: '',
            canLoadMore: false,
            pendingTarget: null
        };
    },
    computed: {
        conversations() {
            return this.$chatState.conversations || [];
        },
        activeConversationId() {
            return this.$chatState.activeConversationId;
        },
        activeConversation() {
            if (!this.activeConversationId) {
                return null;
            }
            return this.conversations.find(item => item && item.id === this.activeConversationId) || null;
        },
        messages() {
            if (!this.activeConversationId) {
                return [];
            }
            const list = this.$chatState.messages[this.activeConversationId];
            return Array.isArray(list) ? list : [];
        },
        currentUserId() {
            return this.$chatState.currentUserId;
        },
        unreadTotal() {
            return this.$chatState.unreadTotal || 0;
        }
    },
    watch: {
        conversations: {
            deep: true,
            handler() {
                this.handleRouteParams();
            }
        },
        activeConversationId(newVal) {
            if (newVal) {
                this.loadMessages(newVal);
            }
        },
        '$route'(to) {
            if (to) {
                this.handleRouteParams();
            }
        }
    },
    mounted() {
        this.initChat();
        this.handleRouteParams();
    },
    methods: {
        async initChat() {
            await this.$chatStore.fetchConversations();
            this.$chatStore.connectIfNeeded();
        },
        async refreshConversations() {
            await this.$chatStore.fetchConversations();
        },
        async loadMessages(conversationId) {
            if (!conversationId) {
                return;
            }
            this.loadingMessages = true;
            try {
                const batch = await this.$chatStore.fetchMessages(conversationId);
                this.canLoadMore = Array.isArray(batch) && batch.length >= 20;
                await this.$chatStore.markConversationRead(conversationId);
            } finally {
                this.loadingMessages = false;
            }
        },
        async loadMoreMessages() {
            if (!this.activeConversationId || this.loadingMessages) {
                return;
            }
            const currentMessages = this.messages;
            const anchor = currentMessages.length ? currentMessages[0].id : null;
            if (!anchor) {
                return;
            }
            this.loadingMessages = true;
            try {
                const batch = await this.$chatStore.fetchMessages(this.activeConversationId, anchor);
                this.canLoadMore = Array.isArray(batch) && batch.length >= 20;
            } finally {
                this.loadingMessages = false;
            }
        },
        selectConversation(conversation) {
            if (!conversation || conversation.id === this.activeConversationId) {
                return;
            }
            this.pendingTarget = null;
            this.$chatStore.setActiveConversationId(conversation.id);
        },
        async handleSend() {
            const content = (this.messageDraft || '').trim();
            if (!content) {
                this.$message.warning('请输入聊天内容');
                return;
            }
            const receiverId = this.resolveReceiverId();
            if (!receiverId) {
                this.$message.warning('请选择聊天对象');
                return;
            }
            this.sendingMessage = true;
            try {
                const message = await this.$chatStore.sendMessage({
                    receiverId,
                    idleId: this.resolveIdleId(),
                    messageType: 0,
                    content: content,
                    extraPayload: null
                });
                this.messageDraft = '';
                if (message && message.conversationId) {
                    this.pendingTarget = null;
                    this.$chatStore.setActiveConversationId(message.conversationId);
                    await this.$chatStore.markConversationRead(message.conversationId);
                }
            } catch (error) {
                this.$message.error('发送失败，请稍后重试');
            } finally {
                this.sendingMessage = false;
            }
        },
        resolvePeerName(conversation) {
            if (!conversation) {
                return '';
            }
            const peer = this.resolvePeer(conversation);
            if (peer && peer.nickname) {
                return peer.nickname;
            }
            return `用户${peer && peer.id ? ` ${peer.id}` : ''}`;
        },
        resolvePeer(conversation) {
            if (!conversation) {
                return null;
            }
            if (conversation.userAId === this.currentUserId) {
                return conversation.userB || null;
            }
            return conversation.userA || null;
        },
        formatPreview(conversation) {
            if (!conversation || !conversation.lastMessagePreview) {
                return '暂无消息';
            }
            return conversation.lastMessagePreview;
        },
        formatTimestamp(time) {
            if (!time) {
                return '';
            }
            if (typeof time === 'string') {
                return time.replace('T', ' ').substring(5, 16);
            }
            try {
                const date = new Date(time);
                return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
            } catch (e) {
                return '';
            }
        },
        conversationUnread(conversation) {
            if (!conversation) {
                return 0;
            }
            if (conversation.userAId === this.currentUserId) {
                return conversation.userAUnread || 0;
            }
            if (conversation.userBId === this.currentUserId) {
                return conversation.userBUnread || 0;
            }
            return 0;
        },
        resolveReceiverId() {
            if (this.activeConversation) {
                const peer = this.resolvePeer(this.activeConversation);
                return peer ? peer.id : null;
            }
            if (this.pendingTarget) {
                return Number(this.pendingTarget.targetId);
            }
            return null;
        },
        resolveIdleId() {
            if (this.activeConversation && this.activeConversation.idleId) {
                return this.activeConversation.idleId;
            }
            if (this.pendingTarget && this.pendingTarget.idleId) {
                return Number(this.pendingTarget.idleId);
            }
            return null;
        },
        handleRouteParams() {
            const { targetId, idleId, productId } = this.$route.query;
            if (!targetId) {
                return;
            }
            const numericTarget = Number(targetId);
            const numericIdle = Number(idleId || productId);
            const existing = this.conversations.find(item => {
                if (!item) {
                    return false;
                }
                const peerId = item.userAId === this.currentUserId ? item.userBId : item.userAId;
                if (peerId !== numericTarget) {
                    return false;
                }
                if (numericIdle && item.idleId) {
                    return Number(item.idleId) === numericIdle;
                }
                return true;
            });
            if (existing) {
                this.pendingTarget = null;
                this.$chatStore.setActiveConversationId(existing.id);
                return;
            }
            this.pendingTarget = {
                targetId: numericTarget,
                idleId: numericIdle || null
            };
        },
        viewIdleDetail(id) {
            if (!id) {
                return;
            }
            this.$router.push({ path: '/details', query: { id } });
        }
    }
};
</script>

<style scoped>
.chat-page-wrapper {
    min-height: 100vh;
    background: #f5f7fa;
}

.chat-layout {
    display: flex;
    height: calc(100vh - 120px);
    margin-top: 20px;
    background: #ffffff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.chat-sidebar {
    width: 280px;
    border-right: 1px solid #ebeef5;
    display: flex;
    flex-direction: column;
    background: #fafafa;
}

.chat-sidebar-header {
    padding: 16px;
    border-bottom: 1px solid #ebeef5;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.chat-sidebar-title {
    font-size: 16px;
    font-weight: 600;
}

.chat-sidebar-unread {
    font-size: 12px;
    color: #f56c6c;
}

.chat-conversation-scroll {
    flex: 1;
}

.chat-conversation-item {
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background 0.2s;
}

.chat-conversation-item:hover {
    background: #f1f5ff;
}

.chat-conversation-item.active {
    background: #e6f0ff;
}

.chat-conversation-name {
    font-weight: 600;
    margin-bottom: 4px;
    color: #303133;
}

.chat-conversation-preview {
    font-size: 13px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.chat-conversation-meta {
    margin-top: 6px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #c0c4cc;
}

.chat-conversation-badge :deep(.el-badge__content) {
    background: #f56c6c;
}

.chat-conversation-empty {
    padding: 24px;
    text-align: center;
    color: #909399;
}

.chat-content {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.chat-content-header {
    padding: 16px;
    border-bottom: 1px solid #ebeef5;
}

.chat-peer-name {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
}

.chat-peer-desc {
    font-size: 13px;
    color: #909399;
    margin-top: 6px;
}

.chat-empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #909399;
    gap: 12px;
}

.chat-empty-title {
    font-size: 18px;
    font-weight: 600;
    color: #606266;
}

.chat-empty-subtitle {
    font-size: 13px;
}
</style>
