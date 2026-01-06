package com.second.hand.trading.server.dao;

import com.second.hand.trading.server.model.ChatConversationModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatConversationDao {
    int deleteByPrimaryKey(Long id);

    int insert(ChatConversationModel record);

    int insertSelective(ChatConversationModel record);

    ChatConversationModel selectByPrimaryKey(Long id);

    ChatConversationModel selectByPair(@Param("userAId") Long userAId,
                                       @Param("userBId") Long userBId,
                                       @Param("idleId") Long idleId);

    List<ChatConversationModel> listByUserId(@Param("userId") Long userId);

    int updateByPrimaryKeySelective(ChatConversationModel record);

    int updateByPrimaryKey(ChatConversationModel record);

    int updateUnreadForSend(@Param("conversationId") Long conversationId,
                            @Param("userAUnread") Integer userAUnread,
                            @Param("userBUnread") Integer userBUnread,
                            @Param("lastMessagePreview") String lastMessagePreview,
                            @Param("lastMessageTime") java.util.Date lastMessageTime,
                            @Param("contextSnapshot") String contextSnapshot);

    int resetUnreadForUserA(@Param("conversationId") Long conversationId);

    int resetUnreadForUserB(@Param("conversationId") Long conversationId);
}
