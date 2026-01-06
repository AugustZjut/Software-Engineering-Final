<template>
    <div class="chat-message-container" ref="container" @scroll="handleScroll">
        <div v-if="loading" class="chat-message-loading">加载中...</div>
        <div v-for="message in messages"
             :key="message.id"
             class="chat-message-row"
             :class="{'from-self': message.senderId === currentUserId}">
            <el-avatar
                class="chat-message-avatar"
                :src="resolveAvatar(message)"
                size="small">
            </el-avatar>
            <div class="chat-message-bubble">
                <div class="chat-message-content">
                    <template v-if="message.messageType === 0">
                        <div class="chat-message-text" v-text="message.content"></div>
                    </template>
                    <template v-else-if="message.messageType === 1">
                        <el-image
                            class="chat-message-image"
                            :src="resolveImageSrc(message)"
                            fit="cover"
                            :preview-src-list="resolveImagePreview(message)">
                        </el-image>
                    </template>
                    <template v-else-if="message.messageType === 2">
                        <div class="chat-message-product">
                            <el-image
                                class="chat-message-product-cover"
                                :src="resolveProductCover(message)"
                                fit="cover"
                                :preview-src-list="resolveProductPreview(message)">
                            </el-image>
                            <div class="chat-message-product-info">
                                <div class="chat-message-product-title">{{ resolveProductTitle(message) }}</div>
                                <div class="chat-message-product-price" v-if="resolveProductPrice(message)">
                                    ￥{{ resolveProductPrice(message) }}
                                </div>
                                <el-button
                                    class="chat-message-product-button"
                                    type="primary"
                                    text
                                    size="small"
                                    @click="emitViewIdle(message)">
                                    查看商品
                                </el-button>
                            </div>
                        </div>
                    </template>
                    <template v-else-if="message.messageType === 3">
                        <div class="chat-message-system">
                            <div class="chat-message-system-title">{{ resolveSystemTitle(message) }}</div>
                            <div class="chat-message-system-body">{{ resolveSystemBody(message) }}</div>
                            <el-button
                                v-if="resolveSystemOrderId(message)"
                                type="primary"
                                text
                                size="small"
                                @click="emitViewOrder(resolveSystemOrderId(message))">
                                查看订单
                            </el-button>
                        </div>
                    </template>
                    <template v-else>
                        <div class="chat-message-text" v-text="message.content"></div>
                    </template>
                </div>
                <div class="chat-message-time">{{ formatTime(message.sendTime) }}</div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'ChatMessageList',
    props: {
        messages: {
            type: Array,
            default: () => []
        },
        currentUserId: {
            type: Number,
            default: null
        },
        loading: {
            type: Boolean,
            default: false
        },
        canLoadMore: {
            type: Boolean,
            default: false
        }
    },
    emits: ['load-more', 'view-idle', 'view-order'],
    data() {
        return {
            autoScroll: true
        };
    },
    watch: {
        messages: {
            deep: true,
            handler() {
                this.$nextTick(() => {
                    if (this.autoScroll) {
                        this.scrollToBottom();
                    }
                });
            }
        }
    },
    methods: {
        handleScroll() {
            const container = this.$refs.container;
            if (!container) {
                return;
            }
            const { scrollTop, scrollHeight, clientHeight } = container;
            this.autoScroll = scrollHeight - scrollTop - clientHeight <= 60;
            if (scrollTop === 0 && this.canLoadMore && !this.loading) {
                this.$emit('load-more');
            }
        },
        scrollToBottom() {
            const container = this.$refs.container;
            if (!container) {
                return;
            }
            container.scrollTop = container.scrollHeight;
        },
        resolveAvatar(message) {
            if (!message || !message.sender) {
                return '';
            }
            return message.sender.avatar || '';
        },
        formatTime(time) {
            if (!time) {
                return '';
            }
            if (typeof time === 'string') {
                return time.replace('T', ' ').substring(0, 16);
            }
            try {
                const date = new Date(time);
                return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
            } catch (e) {
                return '';
            }
        },
        resolveExtra(message) {
            if (!message || !message.extraPayloadObject) {
                return {};
            }
            return message.extraPayloadObject;
        },
        resolveImageSrc(message) {
            const extra = this.resolveExtra(message);
            if (extra && extra.url) {
                return extra.url;
            }
            return message && message.content ? message.content : '';
        },
        resolveImagePreview(message) {
            const src = this.resolveImageSrc(message);
            return src ? [src] : [];
        },
        resolveProductCover(message) {
            const extra = this.resolveExtra(message);
            return extra && extra.picture ? extra.picture : '';
        },
        resolveProductPreview(message) {
            const cover = this.resolveProductCover(message);
            return cover ? [cover] : [];
        },
        resolveProductTitle(message) {
            const extra = this.resolveExtra(message);
            return extra && extra.idleName ? extra.idleName : '闲置信息';
        },
        resolveProductPrice(message) {
            const extra = this.resolveExtra(message);
            return extra && extra.idlePrice ? extra.idlePrice : '';
        },
        emitViewIdle(message) {
            const extra = this.resolveExtra(message);
            if (extra && extra.idleId) {
                this.$emit('view-idle', extra.idleId);
            }
        },
        resolveSystemTitle(message) {
            const extra = this.resolveExtra(message);
            if (extra && extra.title) {
                return extra.title;
            }
            if (extra && extra.systemType) {
                return extra.systemType;
            }
            return '系统消息';
        },
        resolveSystemBody(message) {
            const extra = this.resolveExtra(message);
            if (extra && extra.description) {
                return extra.description;
            }
            if (extra && extra.message) {
                return extra.message;
            }
            if (message && message.content) {
                return message.content;
            }
            return '';
        },
        resolveSystemOrderId(message) {
            const extra = this.resolveExtra(message);
            return extra && extra.orderId ? extra.orderId : null;
        },
        emitViewOrder(orderId) {
            if (orderId) {
                this.$emit('view-order', orderId);
            }
        }
    }
};
</script>

<style scoped>
.chat-message-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px 16px;
    background: #f5f7fa;
}

.chat-message-loading {
    text-align: center;
    color: #909399;
    font-size: 13px;
    margin-bottom: 8px;
}

.chat-message-row {
    display: flex;
    align-items: flex-end;
    margin-bottom: 12px;
    max-width: 80%;
}

.chat-message-row.from-self {
    margin-left: auto;
    flex-direction: row-reverse;
}

.chat-message-avatar {
    margin-right: 8px;
}

.chat-message-row.from-self .chat-message-avatar {
    margin-left: 8px;
    margin-right: 0;
}

.chat-message-bubble {
    background: #fff;
    border-radius: 8px;
    padding: 8px 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    max-width: 100%;
}

.chat-message-row.from-self .chat-message-bubble {
    background: #409eff;
    color: #fff;
}

.chat-message-content {
    white-space: pre-wrap;
    word-break: break-word;
    font-size: 14px;
}

.chat-message-text {
    white-space: pre-wrap;
    word-break: break-word;
}

.chat-message-image {
    width: 180px;
    max-height: 220px;
    border-radius: 6px;
    cursor: pointer;
}

.chat-message-product {
    display: flex;
    align-items: center;
    gap: 12px;
}

.chat-message-product-cover {
    width: 72px;
    height: 72px;
    border-radius: 6px;
    background: #f5f7fa;
}

.chat-message-product-info {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 200px;
}

.chat-message-product-title {
    font-weight: 600;
    color: inherit;
}

.chat-message-product-price {
    color: #f56c6c;
    font-weight: 600;
}

.chat-message-product-button {
    padding: 0;
    border: none;
    background: transparent;
    color: inherit;
}

.chat-message-product-button:focus,
.chat-message-product-button:hover {
    background: transparent;
    color: inherit;
}

.chat-message-row.from-self .chat-message-product-button {
    color: #ffffff;
}

.chat-message-row.from-self .chat-message-product-button:hover,
.chat-message-row.from-self .chat-message-product-button:focus {
    color: rgba(255, 255, 255, 0.85);
    background: transparent;
}

.chat-message-system {
    background: rgba(64, 158, 255, 0.12);
    border-radius: 6px;
    padding: 8px 10px;
    color: #303133;
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 260px;
}

.chat-message-row.from-self .chat-message-system {
    background: rgba(255, 255, 255, 0.2);
    color: #ffffff;
}

.chat-message-system-title {
    font-weight: 600;
}

.chat-message-system-body {
    font-size: 13px;
    line-height: 1.5;
    white-space: pre-wrap;
}

.chat-message-time {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 4px;
}

.chat-message-row.from-self .chat-message-time {
    color: rgba(255, 255, 255, 0.7);
    text-align: right;
}
</style>
