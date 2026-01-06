package com.second.hand.trading.server.service;

import com.second.hand.trading.server.model.EvaluationModel;

import java.util.List;

public interface EvaluationService {

    EvaluationModel submitEvaluation(Long evaluatorId, Long orderId, Integer score, String content);

    EvaluationModel getEvaluation(Long evaluatorId, Long orderId);

    List<EvaluationModel> listReceivedEvaluations(Long userId);

    List<EvaluationModel> listGivenEvaluations(Long userId);

    List<EvaluationModel> listByOrderId(Long orderId, Long requesterId);
}
