package com.second.hand.trading.server.service;

import com.second.hand.trading.server.dao.ChatConversationDao;
import com.second.hand.trading.server.dao.ChatMessageDao;
import com.second.hand.trading.server.dao.IdleItemDao;
import com.second.hand.trading.server.dao.UserDao;
import com.second.hand.trading.server.model.ChatConversationModel;
import com.second.hand.trading.server.model.ChatMessageModel;
import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.model.UserModel;
import com.second.hand.trading.server.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private ChatConversationDao chatConversationDao;

    @Mock
    private ChatMessageDao chatMessageDao;

    @Mock
    private UserDao userDao;

    @Mock
    private IdleItemDao idleItemDao;

    private UserModel sender;
    private UserModel receiver;

    @BeforeEach
    void setUp() {
        sender = createUser(1L, "sender");
        receiver = createUser(2L, "receiver");
    }

    @Test
    void sendMessageCreatesConversationWhenMissing() {
        when(chatConversationDao.selectByPair(1L, 2L, 0L)).thenReturn(null);
        doAnswer(invocation -> {
            ChatConversationModel model = invocation.getArgument(0);
            model.setId(100L);
            return 1;
        }).when(chatConversationDao).insertSelective(any(ChatConversationModel.class));
        doAnswer(invocation -> {
            ChatMessageModel message = invocation.getArgument(0);
            message.setId(200L);
            return 1;
        }).when(chatMessageDao).insertSelective(any(ChatMessageModel.class));
        when(userDao.findUserByList(any())).thenReturn(Arrays.asList(sender, receiver));

        ChatMessageModel result = chatService.sendMessage(1L, 2L, null, 0, "hello", null);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getConversationId()).isEqualTo(100L);
        assertThat(result.getSender()).isNotNull();
        assertThat(result.getReceiver()).isNotNull();

        ArgumentCaptor<ChatConversationModel> captor = ArgumentCaptor.forClass(ChatConversationModel.class);
        verify(chatConversationDao).insertSelective(captor.capture());
        ChatConversationModel stored = captor.getValue();
        assertThat(stored.getUserAId()).isEqualTo(1L);
        assertThat(stored.getUserBId()).isEqualTo(2L);
        assertThat(stored.getUserBUnread()).isEqualTo(1);
        verify(chatConversationDao).selectByPair(1L, 2L, 0L);
        verifyNoMoreInteractions(chatConversationDao);
    }

    @Test
    void sendMessageUpdatesUnreadForExistingConversation() {
        ChatConversationModel conversation = new ChatConversationModel();
        conversation.setId(300L);
        conversation.setUserAId(1L);
        conversation.setUserBId(2L);
        conversation.setUserAUnread(2);
        conversation.setUserBUnread(0);
        when(chatConversationDao.selectByPair(1L, 2L, 0L)).thenReturn(conversation);
        when(chatConversationDao.updateUnreadForSend(eq(300L), eq(2), eq(1), any(), any(), any())).thenReturn(1);
        doAnswer(invocation -> {
            ChatMessageModel message = invocation.getArgument(0);
            message.setId(400L);
            return 1;
        }).when(chatMessageDao).insertSelective(any(ChatMessageModel.class));
        when(userDao.findUserByList(any())).thenReturn(Arrays.asList(sender, receiver));

        ChatMessageModel result = chatService.sendMessage(1L, 2L, null, 0, "hi", null);

        assertThat(result.getId()).isEqualTo(400L);
        verify(chatConversationDao).updateUnreadForSend(eq(300L), eq(2), eq(1), any(), any(), any());
    }

    @Test
    void listConversationsHydratesUsersAndIdle() {
        ChatConversationModel conversation = new ChatConversationModel();
        conversation.setId(10L);
        conversation.setUserAId(1L);
        conversation.setUserBId(2L);
        conversation.setIdleId(50L);
        when(chatConversationDao.listByUserId(1L)).thenReturn(Collections.singletonList(conversation));
        when(userDao.findUserByList(any())).thenReturn(Arrays.asList(sender, receiver));
        IdleItemModel idle = new IdleItemModel();
        idle.setId(50L);
        idle.setIdleName("Test Idle");
        idle.setIdlePrice(new BigDecimal("99.99"));
        when(idleItemDao.findIdleByList(any())).thenReturn(Collections.singletonList(idle));

        List<ChatConversationModel> list = chatService.listConversations(1L);

        assertThat(list).hasSize(1);
        ChatConversationModel model = list.get(0);
        assertThat(model.getUserA()).isEqualTo(sender);
        assertThat(model.getUserB()).isEqualTo(receiver);
        assertThat(model.getIdle()).isEqualTo(idle);
        assertThat(model.getIdleId()).isEqualTo(50L);
    }

    @Test
    void resetUnreadClearsCounterAndMarksMessages() {
        ChatConversationModel conversation = new ChatConversationModel();
        conversation.setId(600L);
        conversation.setUserAId(1L);
        conversation.setUserBId(2L);
        when(chatConversationDao.selectByPrimaryKey(600L)).thenReturn(conversation);

        chatService.resetUnread(2L, 600L);

        verify(chatConversationDao).resetUnreadForUserB(600L);
        verify(chatMessageDao).markAsRead(eq(600L), eq(2L), any(Date.class));
        verifyNoInteractions(idleItemDao);
    }

    private UserModel createUser(Long id, String nickname) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar("avatar" + id);
        return user;
    }
}
