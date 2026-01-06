package com.second.hand.trading.server.dao;

import com.second.hand.trading.server.model.ChatMessageModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ChatMessageDao {
    int deleteByPrimaryKey(Long id);

    int insert(ChatMessageModel record);

    int insertSelective(ChatMessageModel record);

    ChatMessageModel selectByPrimaryKey(Long id);

    List<ChatMessageModel> listByConversation(@Param("conversationId") Long conversationId,
                                              @Param("anchorId") Long anchorId,
                                              @Param("pageSize") Integer pageSize);

    int updateByPrimaryKeySelective(ChatMessageModel record);

    int updateByPrimaryKey(ChatMessageModel record);

    int markAsRead(@Param("conversationId") Long conversationId,
                   @Param("receiverId") Long receiverId,
                   @Param("readTime") Date readTime);
}
