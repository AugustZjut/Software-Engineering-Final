package com.second.hand.trading.server.controller;

import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.service.RecommendationService;
import com.second.hand.trading.server.vo.HomeRecommendVo;
import com.second.hand.trading.server.vo.ResultVo;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("recommend")
public class RecommendController {

    @Resource
    private RecommendationService recommendationService;

    @GetMapping("home")
    public ResultVo<HomeRecommendVo> getHomeRecommendations(
            @CookieValue(value = "shUserId", required = false) String shUserId,
            @RequestParam(value = "hotLimit", required = false) Integer hotLimit,
            @RequestParam(value = "latestLimit", required = false) Integer latestLimit,
            @RequestParam(value = "localLimit", required = false) Integer localLimit) {
        Long userId = parseUserId(shUserId);
        HomeRecommendVo data = recommendationService.getHomeRecommendations(userId, hotLimit, latestLimit, localLimit);
        return ResultVo.success(data);
    }

    @GetMapping("content")
    public ResultVo<List<IdleItemModel>> getContentRecommendations(
            @RequestParam("idleId") Long idleId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<IdleItemModel> data = recommendationService.getContentBasedRecommendations(idleId, limit);
        return ResultVo.success(data);
    }

    private Long parseUserId(String shUserId) {
        if (shUserId == null || shUserId.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(shUserId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
