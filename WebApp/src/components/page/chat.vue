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
                            @load-more="loadMoreMessages"
                            @view-idle="handleViewIdle"
                            @view-order="handleViewOrder">
                        </chat-message-list>
                        <chat-input
                            v-model="messageDraft"
                            :sending="sendingMessage"
                            @send="handleSend"
                            @send-image="handleSendImage"
                            @send-product="handleSendProduct"
                            @send-order="handleSendOrder">
                        </chat-input>
                    </template>
                    <template v-else-if="pendingView">
                        <div class="chat-content-header">
                            <div class="chat-peer-name">{{ pendingPeerName }}</div>
                            <div class="chat-peer-desc">{{ pendingHeaderDesc }}</div>
                        </div>
                        <chat-context-bar
                            :idle="pendingContextIdle"
                            @view-detail="viewIdleDetail">
                        </chat-context-bar>
                        <chat-message-list
                            :messages="pendingMessages"
                            :current-user-id="currentUserId"
                            :loading="pendingLoading"
                            :can-load-more="false"
                            @view-idle="handleViewIdle"
                            @view-order="handleViewOrder">
                        </chat-message-list>
                        <chat-input
                            v-model="messageDraft"
                            :sending="sendingMessage"
                            @send="handleSend"
                            @send-image="handleSendImage"
                            @send-product="handleSendProduct"
                            @send-order="handleSendOrder">
                        </chat-input>
                    </template>
                    <template v-else>
                        <div class="chat-empty-state">
                            <div class="chat-empty-title">请选择一个会话</div>
                            <div class="chat-empty-subtitle">从列表中选择已有会话，或在商品详情页点击“联系卖家”开始聊天。</div>
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
            pendingTarget: null,
            pendingIdle: null,
            pendingSeller: null,
            pendingMessages: [],
            pendingLoading: false
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
        },
        pendingView() {
            return !!(this.pendingTarget && !this.activeConversation);
        },
        pendingPeerName() {
            if (this.pendingSeller && this.pendingSeller.nickname) {
                return this.pendingSeller.nickname;
            }
            if (!this.pendingTarget) {
                return '';
            }
            return `用户 ${this.pendingTarget.targetId}`;
        },
        pendingHeaderDesc() {
            if (this.pendingIdle && this.pendingIdle.idleName) {
                return `咨询闲置：${this.pendingIdle.idleName}`;
            }
            return '发送首条消息即可建立会话';
        },
        pendingContextIdle() {
            return this.pendingIdle || null;
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
            this.resetPendingState();
            this.$chatStore.preparePendingConversation(null);
            this.$chatStore.setActiveConversationId(conversation.id);
            this.updateChatRoute({ conversationId: conversation.id });
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
                await this.afterSendSuccess(message);
            } catch (error) {
                this.$message.error('发送失败，请稍后重试');
            } finally {
                this.sendingMessage = false;
            }
        },
        async handleSendImage(payload) {
            const receiverId = this.resolveReceiverId();
            if (!receiverId) {
                this.$message.warning('请选择聊天对象');
                return;
            }
            const url = payload && payload.url ? payload.url : '';
            if (!url) {
                this.$message.error('上传图片失败');
                return;
            }
            this.sendingMessage = true;
            try {
                const message = await this.$chatStore.sendMessage({
                    receiverId,
                    idleId: this.resolveIdleId(),
                    messageType: 1,
                    content: url,
                    extraPayload: { url }
                });
                await this.afterSendSuccess(message);
            } catch (error) {
                this.$message.error('图片发送失败，请稍后重试');
            } finally {
                this.sendingMessage = false;
            }
        },
        handleSendProduct() {
            this.$message.info('商品卡片发送功能开发中');
        },
        handleSendOrder() {
            this.$message.info('订单消息功能开发中');
        },
        async afterSendSuccess(message) {
            if (!message || !message.conversationId) {
                return;
            }
            this.resetPendingState();
            this.$chatStore.preparePendingConversation(null);
            this.$chatStore.setActiveConversationId(message.conversationId);
            await this.$chatStore.markConversationRead(message.conversationId);
            this.updateChatRoute({ conversationId: message.conversationId });
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
            if (this.pendingIdle && this.pendingIdle.id) {
                return this.pendingIdle.id;
            }
            return null;
        },
        async handleRouteParams() {
            const query = this.$route.query || {};
            const { targetId } = query;
            const idleSource = query.idleId ?? query.productId ?? null;

            const { conversationId } = query;

            if (!targetId && conversationId) {
                const numericConversationId = Number(conversationId);
                const match = this.conversations.find(item => item && item.id === numericConversationId) || null;
                if (match) {
                    this.resetPendingState();
                    this.$chatStore.preparePendingConversation(null);
                    this.$chatStore.setActiveConversationId(match.id);
                    this.updateChatRoute({ conversationId: match.id });
                    return;
                }
            }

            if (!targetId) {
                this.resetPendingState();
                this.$chatStore.preparePendingConversation(null);
                if (!conversationId) {
                    this.updateChatRoute();
                }
                return;
            }

            const numericTarget = Number(targetId);
            if (!Number.isFinite(numericTarget) || numericTarget <= 0) {
                this.resetPendingState();
                this.$chatStore.preparePendingConversation(null);
                this.updateChatRoute();
                return;
            }

            let normalizedIdle = null;
            if (idleSource !== null && idleSource !== undefined && idleSource !== '') {
                const idleNumber = Number(idleSource);
                if (Number.isFinite(idleNumber) && idleNumber > 0) {
                    normalizedIdle = idleNumber;
                }
            }

            const existing = this.conversations.find(item => {
                if (!item) {
                    return false;
                }
                const peerId = item.userAId === this.currentUserId ? item.userBId : item.userAId;
                if (peerId !== numericTarget) {
                    return false;
                }
                if (normalizedIdle && item.idleId) {
                    return Number(item.idleId) === normalizedIdle;
                }
                return true;
            });

            if (existing && existing.id) {
                this.resetPendingState();
                this.$chatStore.preparePendingConversation(null);
                this.$chatStore.setActiveConversationId(existing.id);
                this.updateChatRoute({ conversationId: existing.id });
                return;
            }

            this.$chatStore.setActiveConversationId(null);
            this.canLoadMore = false;

            this.pendingTarget = {
                targetId: numericTarget,
                idleId: normalizedIdle
            };
            this.pendingIdle = null;
            this.pendingSeller = null;
            this.pendingMessages = [];
            this.pendingLoading = true;
            this.$chatStore.preparePendingConversation(numericTarget, normalizedIdle);
            await this.loadPendingContext(numericTarget, normalizedIdle);
        },
        viewIdleDetail(id) {
            if (!id) {
                return;
            }
            this.$router.push({ path: '/details', query: { id } });
        },
        handleViewIdle(id) {
            if (!id) {
                return;
            }
            this.viewIdleDetail(id);
        },
        handleViewOrder(orderId) {
            if (!orderId) {
                return;
            }
            this.$router.push({ path: '/order', query: { id: orderId } });
        },
        async loadPendingContext(targetId, idleId) {
            try {
                let idleData = null;
                let sellerData = null;
                if (idleId) {
                    const res = await this.$api.getIdleItem({ id: idleId });
                    if (res && res.status_code === 1 && res.data) {
                        idleData = res.data;
                        if (res.data.user) {
                            sellerData = res.data.user;
                        }
                    }
                }
                if (!sellerData) {
                    sellerData = this.findUserFromConversations(targetId);
                }
                if (!this.pendingTarget || this.pendingTarget.targetId !== targetId) {
                    return;
                }
                this.pendingIdle = idleData;
                this.pendingSeller = sellerData;
                this.pendingMessages = this.buildPendingIntroMessages();
            } catch (error) {
                console.error('loadPendingContext error', error);
                if (this.pendingTarget && this.pendingTarget.targetId === targetId) {
                    this.pendingIdle = null;
                    this.pendingSeller = null;
                    this.pendingMessages = this.buildPendingIntroMessages();
                }
            } finally {
                if (this.pendingTarget && this.pendingTarget.targetId === targetId) {
                    this.pendingLoading = false;
                }
            }
        },
        findUserFromConversations(userId) {
            const match = this.conversations.find(item => {
                if (!item) {
                    return false;
                }
                if (item.userA && item.userA.id === userId) {
                    return true;
                }
                if (item.userB && item.userB.id === userId) {
                    return true;
                }
                return false;
            });
            if (!match) {
                return null;
            }
            if (match.userA && match.userA.id === userId) {
                return match.userA;
            }
            if (match.userB && match.userB.id === userId) {
                return match.userB;
            }
            return null;
        },
        buildPendingIntroMessages() {
            const name = this.pendingIdle && this.pendingIdle.idleName ? this.pendingIdle.idleName : '';
            const content = name
                ? `你正在咨询闲置《${name}》，发送首条消息开始聊天。`
                : '发送首条消息开始与对方沟通。';
            return [
                {
                    id: 'pending-intro',
                    senderId: 0,
                    sender: {
                        avatar: '',
                        nickname: '系统提示'
                    },
                    content,
                    sendTime: new Date().toISOString()
                }
            ];
        },
        resetPendingState() {
            this.pendingTarget = null;
            this.pendingIdle = null;
            this.pendingSeller = null;
            this.pendingMessages = [];
            this.pendingLoading = false;
        },
        updateChatRoute(query) {
            const normalized = {};
            if (query) {
                Object.keys(query).forEach(key => {
                    const value = query[key];
                    if (value !== null && value !== undefined && value !== '') {
                        normalized[key] = String(value);
                    }
                });
            }
            const current = this.$route.query || {};
            const currentKeys = Object.keys(current);
            const normalizedKeys = Object.keys(normalized);
            let identical = currentKeys.length === normalizedKeys.length;
            if (identical) {
                for (const key of normalizedKeys) {
                    if (current[key] !== normalized[key]) {
                        identical = false;
                        break;
                    }
                }
                if (identical) {
                    for (const key of currentKeys) {
                        if (!normalizedKeys.includes(key)) {
                            identical = false;
                            break;
                        }
                    }
                }
            }
            if (identical) {
                return;
            }
            this.$router.replace({
                path: '/chat',
                query: normalized
            });
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
