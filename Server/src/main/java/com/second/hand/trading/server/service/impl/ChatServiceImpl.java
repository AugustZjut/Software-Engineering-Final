package com.second.hand.trading.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.second.hand.trading.server.Exception.ParamException;
import com.second.hand.trading.server.dao.ChatConversationDao;
import com.second.hand.trading.server.dao.ChatMessageDao;
import com.second.hand.trading.server.dao.IdleItemDao;
import com.second.hand.trading.server.dao.UserDao;
import com.second.hand.trading.server.model.ChatConversationModel;
import com.second.hand.trading.server.model.ChatMessageModel;
import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.model.UserModel;
import com.second.hand.trading.server.service.ChatService;
import com.second.hand.trading.server.enums.ChatMessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Resource
    private ChatConversationDao chatConversationDao;

    @Resource
    private ChatMessageDao chatMessageDao;

    @Resource
    private UserDao userDao;

    @Resource
    private IdleItemDao idleItemDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageModel sendMessage(Long senderId,
                                        Long receiverId,
                                        Long idleId,
                                        Integer messageType,
                                        String content,
                                        String extraPayload) {
        ChatMessageType type = validateSendParameters(senderId, receiverId, messageType, content, extraPayload, idleId);
        PayloadNormalization payloadNormalization = normalizeExtraPayload(type, extraPayload, idleId);
        long bindIdleId = resolveIdleId(payloadNormalization.getResolvedIdleId(), idleId);
        ConversationKey key = ConversationKey.of(senderId, receiverId, bindIdleId);
        ChatConversationModel conversation = chatConversationDao.selectByPair(key.userAId, key.userBId, key.idleId);
        Date now = currentDate();
        String normalizedExtra = payloadNormalization.getPayload();
        String normalizedContent = normalizeContent(type, content, normalizedExtra);
        String contextSnapshot = ensureContextSnapshot(bindIdleId, conversation);
        String preview = buildPreview(type, normalizedContent, normalizedExtra);
        if (conversation == null) {
            ChatConversationModel toCreate = new ChatConversationModel();
            toCreate.setUserAId(key.userAId);
            toCreate.setUserBId(key.userBId);
            toCreate.setIdleId(key.idleId);
            toCreate.setContextSnapshot(contextSnapshot);
            toCreate.setLastMessagePreview(preview);
            toCreate.setLastMessageTime(now);
            toCreate.setUserAUnread(Objects.equals(receiverId, key.userAId) ? 1 : 0);
            toCreate.setUserBUnread(Objects.equals(receiverId, key.userBId) ? 1 : 0);
            toCreate.setCreateTime(now);
            toCreate.setUpdateTime(now);
            chatConversationDao.insertSelective(toCreate);
            conversation = toCreate;
        } else {
            int userAUnread = safeAdd(conversation.getUserAUnread(), Objects.equals(receiverId, key.userAId) ? 1 : 0);
            int userBUnread = safeAdd(conversation.getUserBUnread(), Objects.equals(receiverId, key.userBId) ? 1 : 0);
            chatConversationDao.updateUnreadForSend(conversation.getId(), userAUnread, userBUnread, preview, now,
                    contextSnapshot != null ? contextSnapshot : conversation.getContextSnapshot());
            conversation.setUserAUnread(userAUnread);
            conversation.setUserBUnread(userBUnread);
            conversation.setLastMessagePreview(preview);
            conversation.setLastMessageTime(now);
            if (contextSnapshot != null) {
                conversation.setContextSnapshot(contextSnapshot);
            }
        }
        ChatMessageModel message = new ChatMessageModel();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setIdleId(bindIdleId == 0 ? null : bindIdleId);
        message.setMessageType(type.getCode());
        message.setContent(normalizedContent);
        message.setExtraPayload(normalizedExtra);
        message.setSendTime(now);
        chatMessageDao.insertSelective(message);
        hydrateUsers(Collections.singletonList(message));
        message.setConversationId(conversation.getId());
        return message;
    }

    @Override
    public List<ChatConversationModel> listConversations(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<ChatConversationModel> conversations = chatConversationDao.listByUserId(userId);
        if (CollectionUtils.isEmpty(conversations)) {
            return conversations;
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> idleIds = new HashSet<>();
        for (ChatConversationModel conversation : conversations) {
            userIds.add(conversation.getUserAId());
            userIds.add(conversation.getUserBId());
            if (conversation.getIdleId() != null && conversation.getIdleId() > 0) {
                idleIds.add(conversation.getIdleId());
            }
        }
        Map<Long, UserModel> userMap = fetchUsers(userIds);
        Map<Long, IdleItemModel> idleMap = fetchIdleItems(idleIds);
        for (ChatConversationModel conversation : conversations) {
            conversation.setUserA(userMap.get(conversation.getUserAId()));
            conversation.setUserB(userMap.get(conversation.getUserBId()));
            Long idleId = conversation.getIdleId();
            if (idleId != null && idleId > 0) {
                conversation.setIdle(idleMap.get(idleId));
            } else {
                conversation.setIdleId(null);
            }
        }
        return conversations;
    }

    @Override
    public List<ChatMessageModel> listMessages(Long userId,
                                               Long conversationId,
                                               Long anchorId,
                                               Integer pageSize) {
        ChatConversationModel conversation = chatConversationDao.selectByPrimaryKey(conversationId);
        if (!isConversationParticipant(conversation, userId)) {
            throw new ParamException(buildError("conversationId", "会话不存在或无权限"));
        }
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 100);
        List<ChatMessageModel> messages = chatMessageDao.listByConversation(conversationId, anchorId, size);
        if (CollectionUtils.isEmpty(messages)) {
            return messages;
        }
        Collections.reverse(messages);
        hydrateUsers(messages);
        return messages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUnread(Long userId, Long conversationId) {
        ChatConversationModel conversation = chatConversationDao.selectByPrimaryKey(conversationId);
        if (!isConversationParticipant(conversation, userId)) {
            throw new ParamException(buildError("conversationId", "会话不存在或无权限"));
        }
        Date now = currentDate();
        if (Objects.equals(userId, conversation.getUserAId())) {
            chatConversationDao.resetUnreadForUserA(conversationId);
        } else {
            chatConversationDao.resetUnreadForUserB(conversationId);
        }
        chatMessageDao.markAsRead(conversationId, userId, now);
    }

    private ChatMessageType validateSendParameters(Long senderId,
                                                   Long receiverId,
                                                   Integer messageType,
                                                   String content,
                                                   String extraPayload,
                                                   Long idleId) {
        if (senderId == null || receiverId == null) {
            throw new ParamException(buildError("userId", "用户不能为空"));
        }
        if (Objects.equals(senderId, receiverId)) {
            throw new ParamException(buildError("receiverId", "不能给自己发送消息"));
        }
        ChatMessageType type = ChatMessageType.fromCode(messageType);
        if (type == ChatMessageType.TEXT && !StringUtils.hasText(content)) {
            throw new ParamException(buildError("content", "消息内容不能为空"));
        }
        if (type == ChatMessageType.IMAGE && !StringUtils.hasText(extraPayload)) {
            throw new ParamException(buildError("extraPayload", "图片地址不能为空"));
        }
        if (type == ChatMessageType.PRODUCT_CARD && (idleId == null || idleId <= 0) && !StringUtils.hasText(extraPayload)) {
            throw new ParamException(buildError("idleId", "请选择要分享的闲置"));
        }
        if (type == ChatMessageType.SYSTEM && !StringUtils.hasText(extraPayload) && !StringUtils.hasText(content)) {
            throw new ParamException(buildError("extraPayload", "系统消息内容不能为空"));
        }
        return type;
    }

    private String ensureContextSnapshot(long idleId, ChatConversationModel conversation) {
        if (idleId <= 0) {
            return null;
        }
        if (conversation != null && StringUtils.hasText(conversation.getContextSnapshot())) {
            return conversation.getContextSnapshot();
        }
        IdleItemModel idleItem = idleItemDao.selectByPrimaryKey(idleId);
        if (idleItem == null) {
            return null;
        }
        Map<String, Object> snapshot = new HashMap<>(8);
        snapshot.put("idleId", idleItem.getId());
        snapshot.put("idleName", idleItem.getIdleName());
        snapshot.put("idlePrice", extractPrice(idleItem.getIdlePrice()));
        snapshot.put("pictureList", idleItem.getPictureList());
        snapshot.put("sellerId", idleItem.getUserId());
        return JSON.toJSONString(snapshot);
    }

    private void hydrateUsers(List<ChatMessageModel> messages) {
        Set<Long> ids = new HashSet<>();
        for (ChatMessageModel message : messages) {
            ids.add(message.getSenderId());
            ids.add(message.getReceiverId());
        }
        Map<Long, UserModel> userMap = fetchUsers(ids);
        for (ChatMessageModel message : messages) {
            message.setSender(userMap.get(message.getSenderId()));
            message.setReceiver(userMap.get(message.getReceiverId()));
        }
    }

    private Map<Long, UserModel> fetchUsers(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<UserModel> users = userDao.findUserByList(new ArrayList<>(ids));
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(UserModel::getId, u -> u));
    }

    private Map<Long, IdleItemModel> fetchIdleItems(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<IdleItemModel> idles = idleItemDao.findIdleByList(new ArrayList<>(ids));
        if (CollectionUtils.isEmpty(idles)) {
            return Collections.emptyMap();
        }
        return idles.stream().collect(Collectors.toMap(IdleItemModel::getId, i -> i));
    }

    private boolean isConversationParticipant(ChatConversationModel conversation, Long userId) {
        return conversation != null && userId != null
                && (Objects.equals(conversation.getUserAId(), userId) || Objects.equals(conversation.getUserBId(), userId));
    }

    private Map<String, String> buildError(String field, String message) {
        Map<String, String> map = new HashMap<>(2);
        map.put(field, message);
        return map;
    }

    private Date currentDate() {
        return Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
    }

    private int safeAdd(Integer origin, int delta) {
        int base = origin == null ? 0 : origin;
        int value = base + delta;
        return Math.max(value, 0);
    }

    private PayloadNormalization normalizeExtraPayload(ChatMessageType type,
                                                       String extraPayload,
                                                       Long idleId) {
        if (type == ChatMessageType.TEXT) {
            return PayloadNormalization.of(nullIfBlank(extraPayload), idleId);
        }
        if (type == ChatMessageType.IMAGE) {
            JSONObject payload = parseJsonObject(extraPayload, "extraPayload");
            String url = payload.getString("url");
            if (!StringUtils.hasText(url)) {
                throw new ParamException(buildError("extraPayload", "图片地址不能为空"));
            }
            return PayloadNormalization.of(payload.toJSONString(), idleId);
        }
        if (type == ChatMessageType.PRODUCT_CARD) {
            return buildProductCardPayload(idleId, extraPayload);
        }
        if (type == ChatMessageType.SYSTEM) {
            JSONObject payload = parseJsonObject(extraPayload, "extraPayload");
            if (!StringUtils.hasText(payload.getString("title"))
                    && !StringUtils.hasText(payload.getString("description"))
                    && !StringUtils.hasText(payload.getString("message"))) {
                throw new ParamException(buildError("extraPayload", "系统消息内容不能为空"));
            }
            return PayloadNormalization.of(payload.toJSONString(), idleId);
        }
        return PayloadNormalization.of(nullIfBlank(extraPayload), idleId);
    }

    private long resolveIdleId(Long payloadIdleId, Long requestIdleId) {
        Long effective = payloadIdleId != null ? payloadIdleId : requestIdleId;
        return effective == null ? 0L : Math.max(effective, 0L);
    }

    private String normalizeContent(ChatMessageType type, String content, String extraPayload) {
        if (type == ChatMessageType.TEXT) {
            return content == null ? "" : content.trim();
        }
        if (StringUtils.hasText(content)) {
            return content.trim();
        }
        if (type == ChatMessageType.SYSTEM && StringUtils.hasText(extraPayload)) {
            try {
                JSONObject payload = JSON.parseObject(extraPayload);
                String description = payload.getString("description");
                if (StringUtils.hasText(description)) {
                    return description;
                }
                String title = payload.getString("title");
                if (StringUtils.hasText(title)) {
                    return title;
                }
            } catch (Exception ignored) {
                // ignore parsing errors for fallback content
            }
        }
        return "";
    }

    private JSONObject parseJsonObject(String json, String field) {
        if (!StringUtils.hasText(json)) {
            throw new ParamException(buildError(field, "参数不能为空"));
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception ex) {
            throw new ParamException(buildError(field, "JSON格式不正确"));
        }
    }

    private PayloadNormalization buildProductCardPayload(Long idleId, String extraPayload) {
        JSONObject payload = StringUtils.hasText(extraPayload) ? parseJsonObject(extraPayload, "extraPayload") : new JSONObject();
        Long resolvedIdleId = payload.getLong("idleId");
        if (resolvedIdleId == null || resolvedIdleId <= 0) {
            resolvedIdleId = idleId;
        }
        if (resolvedIdleId == null || resolvedIdleId <= 0) {
            throw new ParamException(buildError("idleId", "商品信息不能为空"));
        }
        IdleItemModel idleItem = idleItemDao.selectByPrimaryKey(resolvedIdleId);
        if (idleItem == null) {
            throw new ParamException(buildError("idleId", "闲置不存在"));
        }
        JSONObject snapshot = new JSONObject();
        snapshot.put("idleId", idleItem.getId());
        snapshot.put("idleName", idleItem.getIdleName());
        snapshot.put("idlePrice", extractPrice(idleItem.getIdlePrice()));
        snapshot.put("picture", extractCoverPicture(idleItem.getPictureList()));
        snapshot.put("idleStatus", idleItem.getIdleStatus());
        snapshot.put("sellerId", idleItem.getUserId());
        return PayloadNormalization.of(snapshot.toJSONString(), resolvedIdleId);
    }

    private String extractCoverPicture(String pictures) {
        if (!StringUtils.hasText(pictures)) {
            return null;
        }
        try {
            JSONArray array = JSON.parseArray(pictures);
            if (array == null || array.isEmpty()) {
                return null;
            }
            Object first = array.get(0);
            return first == null ? null : String.valueOf(first);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPreview(ChatMessageType type, String content, String extraPayload) {
        if (type == ChatMessageType.TEXT) {
            return abbreviate(content, 100);
        }
        if (type == ChatMessageType.IMAGE) {
            return "[图片]";
        }
        if (type == ChatMessageType.PRODUCT_CARD) {
            return "[商品卡片]";
        }
        if (type == ChatMessageType.SYSTEM) {
            if (StringUtils.hasText(extraPayload)) {
                try {
                    JSONObject payload = JSON.parseObject(extraPayload);
                    String title = payload.getString("title");
                    if (StringUtils.hasText(title)) {
                        return abbreviate(title, 60);
                    }
                    String systemType = payload.getString("systemType");
                    if (StringUtils.hasText(systemType)) {
                        return "[" + systemType + "]";
                    }
                } catch (Exception ignored) {
                    return "[系统消息]";
                }
            }
            return "[系统消息]";
        }
        return "[消息]";
    }

    private String nullIfBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    private String extractPrice(BigDecimal price) {
        if (price == null) {
            return null;
        }
        return price.stripTrailingZeros().toPlainString();
    }

    private static final class PayloadNormalization {
        private final String payload;
        private final Long resolvedIdleId;

        private PayloadNormalization(String payload, Long resolvedIdleId) {
            this.payload = payload;
            this.resolvedIdleId = resolvedIdleId;
        }

        static PayloadNormalization of(String payload, Long resolvedIdleId) {
            return new PayloadNormalization(payload, resolvedIdleId);
        }

        String getPayload() {
            return payload;
        }

        Long getResolvedIdleId() {
            return resolvedIdleId;
        }
    }

    private static class ConversationKey {
        private final long userAId;
        private final long userBId;
        private final long idleId;

        private ConversationKey(long userAId, long userBId, long idleId) {
            this.userAId = userAId;
            this.userBId = userBId;
            this.idleId = idleId;
        }

        static ConversationKey of(Long first, Long second, long idleId) {
            long a = Math.min(first, second);
            long b = Math.max(first, second);
            return new ConversationKey(a, b, idleId);
        }
    }
}
