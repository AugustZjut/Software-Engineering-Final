<template>
    <div class="chat-context-wrapper">
        <template v-if="idle">
            <el-card shadow="never" class="chat-context-card">
                <div class="chat-context">
                    <el-image
                        class="chat-context-cover"
                        :src="cover"
                        fit="cover">
                    </el-image>
                    <div class="chat-context-info">
                        <div class="chat-context-title" :title="idle.idleName">{{ idle.idleName }}</div>
                        <div class="chat-context-price" v-if="idle.idlePrice">￥{{ formatPrice(idle.idlePrice) }}</div>
                        <el-button type="primary" text @click="$emit('view-detail', idle.id)">查看详情</el-button>
                    </div>
                </div>
            </el-card>
        </template>
        <div v-else class="chat-context-empty">未关联任何闲置商品</div>
    </div>
</template>

<script>
export default {
    name: 'ChatContextBar',
    props: {
        idle: {
            type: Object,
            default: null
        }
    },
    computed: {
        cover() {
            if (!this.idle || !this.idle.pictureList) {
                return '';
            }
            try {
                const list = JSON.parse(this.idle.pictureList);
                return Array.isArray(list) && list.length > 0 ? list[0] : '';
            } catch (e) {
                return '';
            }
        }
    },
    methods: {
        formatPrice(price) {
            if (!price) {
                return '0';
            }
            return String(price);
        }
    }
};
</script>

<style scoped>
.chat-context-wrapper {
    margin-bottom: 12px;
}

.chat-context-card {
    border: 1px solid #f1f1f1;
}

.chat-context {
    display: flex;
    align-items: center;
}

.chat-context-cover {
    width: 72px;
    height: 72px;
    border-radius: 6px;
    background: #f3f4f6;
}

.chat-context-info {
    flex: 1;
    margin-left: 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.chat-context-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.chat-context-price {
    color: #f56c6c;
    font-weight: 600;
}

.chat-context-empty {
    font-size: 13px;
    color: #909399;
    padding: 8px 0;
}
</style>
