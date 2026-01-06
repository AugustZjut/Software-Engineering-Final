package com.second.hand.trading.server.dao;

import com.second.hand.trading.server.model.EvaluationModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EvaluationDao {

    int insertSelective(EvaluationModel record);

    EvaluationModel selectByOrderAndEvaluator(@Param("orderId") Long orderId,
                                              @Param("evaluatorId") Long evaluatorId);

    List<EvaluationModel> selectByTargetId(@Param("targetId") Long targetId);

    List<EvaluationModel> selectByEvaluatorId(@Param("evaluatorId") Long evaluatorId);

    Double selectAverageScoreByTargetId(@Param("targetId") Long targetId);

    List<EvaluationModel> selectByOrderId(@Param("orderId") Long orderId);
}
