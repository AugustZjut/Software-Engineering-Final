<template>
    <div class="chat-input-wrapper">
        <div class="chat-input-toolbar">
            <el-upload
                class="chat-input-upload"
                :action="uploadAction"
                :show-file-list="false"
                :with-credentials="true"
                accept="image/*"
                :disabled="sending"
                @success="handleImageSuccess"
                @error="handleImageError">
                <el-button text type="primary" :disabled="sending">图片</el-button>
            </el-upload>
            <el-button
                text
                type="primary"
                :disabled="sending || !canSendProduct"
                @click="handleSendProduct">
                发送商品卡片
            </el-button>
            <el-button
                text
                type="primary"
                :disabled="sending || !canSendOrder"
                @click="handleSendOrder">
                订单消息
            </el-button>
        </div>
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
        },
        canSendProduct: {
            type: Boolean,
            default: false
        },
        canSendOrder: {
            type: Boolean,
            default: false
        },
        uploadAction: {
            type: String,
            default: '/api/file'
        }
    },
    emits: ['update:modelValue', 'send', 'send-image', 'send-product', 'send-order'],
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
        },
        handleImageSuccess(response, file) {
            const url = response && response.data ? response.data : response && response.url ? response.url : '';
            if (!url) {
                this.$message.error('上传图片失败');
                return;
            }
            this.$emit('send-image', { url, response, file });
        },
        handleImageError() {
            this.$message.error('上传图片失败');
        },
        handleSendProduct() {
            if (this.sending || !this.canSendProduct) {
                return;
            }
            this.$emit('send-product');
        },
        handleSendOrder() {
            if (this.sending || !this.canSendOrder) {
                return;
            }
            this.$emit('send-order');
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

.chat-input-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
}

.chat-input-upload {
    display: inline-flex;
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
