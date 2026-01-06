<template>
    <div class="chat-input-wrapper">
        <el-input
            class="chat-input-textarea"
            type="textarea"
            :rows="3"
            :model-value="modelValue"
            placeholder="请输入聊天内容，Enter发送，Shift+Enter换行"
            @update:modelValue="handleInput"
            @keydown="handleKeydown">
        </el-input>
        <div class="chat-input-actions">
            <div class="chat-input-tip">按 Enter 发送，Shift + Enter 换行</div>
            <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
        </div>
    </div>
</template>

<script>
export default {
    name: 'ChatInput',
    props: {
        modelValue: {
            type: String,
            default: ''
        },
        sending: {
            type: Boolean,
            default: false
        }
    },
    emits: ['update:modelValue', 'send'],
    methods: {
        handleInput(value) {
            this.$emit('update:modelValue', value);
        },
        handleKeydown(event) {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                this.handleSend();
            }
        },
        handleSend() {
            if (this.sending) {
                return;
            }
            this.$emit('send');
        }
    }
};
</script>

<style scoped>
.chat-input-wrapper {
    border-top: 1px solid #e4e7ed;
    padding: 12px 16px;
    background: #fff;
}

.chat-input-textarea :deep(.el-textarea__inner) {
    resize: none;
}

.chat-input-actions {
    margin-top: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.chat-input-tip {
    font-size: 12px;
    color: #909399;
}
</style>
