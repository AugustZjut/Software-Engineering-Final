package com.second.hand.trading.server.service;

import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.vo.HomeRecommendVo;

import java.util.List;

public interface RecommendationService {

    HomeRecommendVo getHomeRecommendations(Long userId, Integer hotLimit,
                                           Integer latestLimit, Integer localLimit);

    List<IdleItemModel> getContentBasedRecommendations(Long idleId, Integer limit);
}
