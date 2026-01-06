import { createApp, reactive, watch } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import $ from 'jquery'
// import 'babel-polyfill'; // Not needed in Vue 3

import api from './api/index.js';

const app = createApp(App);

app.config.globalProperties.$api = api;

const globalData = reactive({
    userInfo: {
        nickname: ''
    }
});
const sta = reactive({
    isLogin: false,
    adminName: ''
});

const chatState = reactive({
    socket: null,
    connected: false,
    reconnectAttempts: 0,
    reconnectTimer: null,
    conversations: [],
    messages: {},
    activeConversationId: null,
    unreadTotal: 0,
    currentUserId: null,
    pendingHint: null
});

const chatStore = {
    state: chatState,
    connectIfNeeded,
    fetchConversations,
    fetchMessages,
    sendMessage,
    markConversationRead,
    setActiveConversationId(id) {
        chatState.activeConversationId = id;
    },
    preparePendingConversation,
    reset: resetChatState
};

app.config.globalProperties.$sta = sta;
app.config.globalProperties.$globalData = globalData;
app.config.globalProperties.$chatStore = chatStore;
app.config.globalProperties.$chatState = chatState;

app.use(ElementPlus, {
    size: 'default'
});

router.beforeEach((to, from, next) => {
    document.title = `${to.meta.title}`;
    // console.log(to.path,'userInfo:', globalData.userInfo);
    const nickname = globalData.userInfo.nickname;
    if (!nickname
        && (to.path === '/me'
            || to.path === '/message'
            || to.path === '/chat'
            || to.path === '/release'
            || to.path === '/order')) {
        api.getUserInfo().then(res => {
            console.log('getUserInfo:', res);
            if (res.status_code !== 1) {
                next('/login');
            } else {
                if (res.data.signInTime) {
                    res.data.signInTime = res.data.signInTime.substring(0, 10);
                }
                // Object.assign to keep reactivity if we replaced the whole object, 
                // but userInfo is a property so direct assignment is fine with reactive()
                globalData.userInfo = res.data;
                if (res.data && res.data.id) {
                    chatState.currentUserId = res.data.id;
                    chatStore.connectIfNeeded();
                    chatStore.fetchConversations();
                }
                next();
            }
        }).catch(e => {
            next('/login');
        });

    } else {
        next();
    }
});

app.use(router);
app.mount('#app');

function resolveWsUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    if (import.meta.env && import.meta.env.DEV) {
        return `${protocol}//localhost:8040/ws/chat`;
    }
    return `${protocol}//${window.location.host}/ws/chat`;
}

function connectChat() {
    if (!chatState.currentUserId) {
        return;
    }
    if (chatState.socket) {
        try {
            chatState.socket.close();
        } catch (e) {
            // ignore
        }
    }
    const socket = new WebSocket(resolveWsUrl());
    chatState.socket = socket;
    socket.onopen = () => {
        chatState.connected = true;
        chatState.reconnectAttempts = 0;
        if (chatState.reconnectTimer) {
            clearTimeout(chatState.reconnectTimer);
            chatState.reconnectTimer = null;
        }
        fetchConversations();
    };
    socket.onmessage = event => handleSocketMessage(event);
    socket.onerror = () => {
        socket.close();
    };
    socket.onclose = () => {
        chatState.connected = false;
        if (chatState.socket === socket) {
            chatState.socket = null;
        }
        scheduleReconnect();
    };
}

function scheduleReconnect() {
    if (!chatState.currentUserId) {
        return;
    }
    if (chatState.reconnectAttempts >= 5) {
        return;
    }
    if (chatState.reconnectTimer) {
        clearTimeout(chatState.reconnectTimer);
    }
    chatState.reconnectAttempts += 1;
    const delay = Math.min(1000 * Math.pow(2, chatState.reconnectAttempts), 10000);
    chatState.reconnectTimer = setTimeout(() => {
        connectChat();
    }, delay);
}

function computeUnread(conversations) {
    if (!Array.isArray(conversations) || !chatState.currentUserId) {
        return 0;
    }
    return conversations.reduce((total, conversation) => {
        if (!conversation) {
            return total;
        }
        if (conversation.userAId === chatState.currentUserId) {
            return total + (conversation.userAUnread || 0);
        }
        if (conversation.userBId === chatState.currentUserId) {
            return total + (conversation.userBUnread || 0);
        }
        return total;
    }, 0);
}

function updateConversations(conversations) {
    chatState.conversations = Array.isArray(conversations) ? conversations : [];
    chatState.unreadTotal = computeUnread(chatState.conversations);
    tryResolvePendingConversation();
}

async function fetchConversations() {
    if (!chatState.currentUserId) {
        return [];
    }
    try {
        const res = await api.getChatConversations();
        if (res.status_code === 1) {
            updateConversations(res.data || []);
        }
    } catch (error) {
        console.error('fetchConversations error', error);
    }
    return chatState.conversations;
}

async function fetchMessages(conversationId, anchorId) {
    if (!conversationId) {
        return [];
    }
    try {
        const res = await api.getChatMessages({
            conversationId,
            anchorId
        });
        if (res.status_code === 1) {
            const list = Array.isArray(res.data) ? res.data.map(enhanceMessage) : [];
            if (anchorId) {
                const existing = chatState.messages[conversationId] || [];
                chatState.messages[conversationId] = [...list, ...existing];
            } else {
                chatState.messages[conversationId] = list;
            }
            return list;
        }
    } catch (error) {
        console.error('fetchMessages error', error);
    }
    return [];
}

async function sendMessage(payload) {
    try {
        const requestPayload = { ...payload };
        if (requestPayload.messageType === undefined || requestPayload.messageType === null) {
            requestPayload.messageType = 0;
        }
        if (requestPayload.extraPayload && typeof requestPayload.extraPayload !== 'string') {
            requestPayload.extraPayload = JSON.stringify(requestPayload.extraPayload);
        }
        const res = await api.sendChatMessage(requestPayload);
        if (res.status_code === 1 && res.data) {
            const normalized = enhanceMessage(res.data);
            appendMessage(normalized.conversationId, normalized);
            markConversationReadLocal(normalized.conversationId);
            fetchConversations();
            chatState.pendingHint = null;
            return normalized;
        }
        return Promise.reject(res);
    } catch (error) {
        console.error('sendMessage error', error);
        return Promise.reject(error);
    }
}

async function markConversationRead(conversationId) {
    if (!conversationId) {
        return;
    }
    try {
        await api.readChatConversation({ conversationId });
    } catch (error) {
        console.error('readConversation error', error);
    }
    markConversationReadLocal(conversationId);
}

function markConversationReadLocal(conversationId) {
    if (!conversationId) {
        return;
    }
    const conversation = chatState.conversations.find(item => item && item.id === conversationId);
    if (!conversation) {
        return;
    }
    if (conversation.userAId === chatState.currentUserId) {
        conversation.userAUnread = 0;
    }
    if (conversation.userBId === chatState.currentUserId) {
        conversation.userBUnread = 0;
    }
    chatState.unreadTotal = computeUnread(chatState.conversations);
}

function appendMessage(conversationId, message) {
    if (!conversationId || !message) {
        return;
    }
    const normalized = enhanceMessage(message);
    const list = chatState.messages[conversationId] ? [...chatState.messages[conversationId]] : [];
    if (normalized.id) {
        const index = list.findIndex(item => item && item.id === normalized.id);
        if (index >= 0) {
            list[index] = normalized;
            chatState.messages[conversationId] = list;
            return;
        }
    }
    list.push(normalized);
    chatState.messages[conversationId] = list;
}

function handleSocketMessage(event) {
    if (!event || !event.data) {
        return;
    }
    let payload;
    try {
        payload = JSON.parse(event.data);
    } catch (error) {
        console.error('chat socket parse error', error);
        return;
    }
    if (payload.event === 'chat_message' && payload.message) {
        const conversationId = payload.conversationId || payload.message.conversationId;
        appendMessage(conversationId, payload.message);
        if (conversationId && chatState.activeConversationId === conversationId) {
            markConversationReadLocal(conversationId);
        } else {
            fetchConversations();
        }
    }
}

function enhanceMessage(message) {
    if (!message) {
        return null;
    }
    const normalized = { ...message };
    normalized.messageType = Number(normalized.messageType === undefined || normalized.messageType === null
        ? 0
        : normalized.messageType);
    if (normalized.extraPayload && typeof normalized.extraPayload === 'object' && normalized.extraPayload !== null) {
        normalized.extraPayloadObject = normalized.extraPayload;
        try {
            normalized.extraPayload = JSON.stringify(normalized.extraPayload);
        } catch (error) {
            console.error('stringify extraPayload error', error);
            normalized.extraPayload = null;
        }
    } else if (typeof normalized.extraPayload === 'string') {
        try {
            normalized.extraPayloadObject = JSON.parse(normalized.extraPayload);
        } catch (error) {
            normalized.extraPayloadObject = null;
        }
    } else {
        normalized.extraPayloadObject = null;
        normalized.extraPayload = normalized.extraPayload || null;
    }
    return normalized;
}

function connectIfNeeded() {
    if (!chatState.currentUserId) {
        return;
    }
    if (chatState.connected || chatState.socket) {
        return;
    }
    connectChat();
}

function preparePendingConversation(targetId, idleId) {
    if (!targetId) {
        chatState.pendingHint = null;
        return;
    }
    chatState.pendingHint = {
        targetId: Number(targetId),
        idleId: idleId ? Number(idleId) : null
    };
    tryResolvePendingConversation();
}

function resetChatState() {
    chatState.currentUserId = null;
    if (chatState.reconnectTimer) {
        clearTimeout(chatState.reconnectTimer);
        chatState.reconnectTimer = null;
    }
    if (chatState.socket) {
        try {
            chatState.socket.close();
        } catch (e) {
            // ignore
        }
    }
    chatState.socket = null;
    chatState.connected = false;
    chatState.reconnectAttempts = 0;
    chatState.conversations = [];
    chatState.messages = {};
    chatState.activeConversationId = null;
    chatState.unreadTotal = 0;
    chatState.pendingHint = null;
}

watch(() => globalData.userInfo && globalData.userInfo.id, (newVal) => {
    sta.isLogin = !!newVal;
    if (newVal) {
        chatState.currentUserId = newVal;
        connectIfNeeded();
        fetchConversations();
    } else {
        resetChatState();
    }
}, { immediate: true });

function tryResolvePendingConversation() {
    const hint = chatState.pendingHint;
    if (!hint || !Array.isArray(chatState.conversations) || !chatState.currentUserId) {
        return;
    }
    const match = chatState.conversations.find(item => {
        if (!item) {
            return false;
        }
        const peerId = item.userAId === chatState.currentUserId ? item.userBId : item.userAId;
        if (peerId !== hint.targetId) {
            return false;
        }
        if (hint.idleId && item.idleId) {
            return Number(item.idleId) === hint.idleId;
        }
        return true;
    });
    if (match && match.id) {
        chatState.activeConversationId = match.id;
        chatState.pendingHint = null;
    }
}
