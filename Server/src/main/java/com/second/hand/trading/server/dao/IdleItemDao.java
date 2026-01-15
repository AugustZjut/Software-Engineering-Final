package com.second.hand.trading.server.dao;

import com.second.hand.trading.server.model.IdleItemModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IdleItemDao {
    int deleteByPrimaryKey(Long id);

    int insert(IdleItemModel record);

    int insertSelective(IdleItemModel record);

    IdleItemModel selectByPrimaryKey(Long id);

    List<IdleItemModel> getAllIdleItem(Long userId);

    int countIdleItem(String findValue);

    int countIdleItemByLable(int idleLabel);

    int countIdleItemByStatus(int status);

    List<IdleItemModel> findIdleItem(String findValue, int begin, int nums);

    List<IdleItemModel> findIdleItemByLable(int idleLabel, int begin, int nums);

    List<IdleItemModel> getIdleItemByStatus(int status, int begin, int nums);

    int updateByPrimaryKeySelective(IdleItemModel record);

    int updateByPrimaryKey(IdleItemModel record);

    List<IdleItemModel> findIdleByList(List<Long> idList);

    List<IdleItemModel> listHotItems(@Param("limit") int limit);

    List<IdleItemModel> listLatestItems(@Param("limit") int limit);

    List<IdleItemModel> listByCity(@Param("idlePlace") String idlePlace,
                                   @Param("limit") int limit);

    List<IdleItemModel> listSimilarItems(@Param("idleLabel") int idleLabel,
                                         @Param("idleId") long idleId,
                                         @Param("idlePlace") String idlePlace,
                                         @Param("searchToken") String searchToken,
                                         @Param("limit") int limit);
}