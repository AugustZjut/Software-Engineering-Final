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
                <div class="chat-message-content" v-text="message.content"></div>
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
    emits: ['load-more'],
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
