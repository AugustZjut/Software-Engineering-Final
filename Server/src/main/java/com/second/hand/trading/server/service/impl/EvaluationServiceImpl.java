package com.second.hand.trading.server.service.impl;

import com.second.hand.trading.server.dao.EvaluationDao;
import com.second.hand.trading.server.dao.IdleItemDao;
import com.second.hand.trading.server.dao.OrderDao;
import com.second.hand.trading.server.dao.UserDao;
import com.second.hand.trading.server.model.EvaluationModel;
import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.model.OrderModel;
import com.second.hand.trading.server.model.UserModel;
import com.second.hand.trading.server.service.EvaluationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;
    private static final int DEFAULT_CREDIT = 80;

    @Resource
    private EvaluationDao evaluationDao;

    @Resource
    private OrderDao orderDao;

    @Resource
    private IdleItemDao idleItemDao;

    @Resource
    private UserDao userDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationModel submitEvaluation(Long evaluatorId, Long orderId, Integer score, String content) {
        if (evaluatorId == null || orderId == null || score == null) {
            return null;
        }
        OrderModel order = orderDao.selectByPrimaryKey(orderId);
        if (order == null || order.getOrderStatus() == null || !Objects.equals(order.getOrderStatus(), (byte) 3)) {
            return null;
        }
        IdleItemModel idleItem = idleItemDao.selectByPrimaryKey(order.getIdleId());
        if (idleItem == null) {
            return null;
        }
        Long buyerId = order.getUserId();
        Long sellerId = idleItem.getUserId();
        Long targetId;
        if (Objects.equals(evaluatorId, buyerId)) {
            targetId = sellerId;
        } else if (Objects.equals(evaluatorId, sellerId)) {
            targetId = buyerId;
        } else {
            return null;
        }
        int normalizedScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
        EvaluationModel model = new EvaluationModel();
        model.setOrderId(orderId);
        model.setEvaluatorId(evaluatorId);
        model.setTargetId(targetId);
        model.setScore(normalizedScore);
        model.setContent(content == null ? null : content.trim());
        model.setCreateTime(new Date());
        if (evaluationDao.insertSelective(model) != 1) {
            return null;
        }
        refreshCreditScore(targetId);
        return evaluationDao.selectByOrderAndEvaluator(orderId, evaluatorId);
    }

    @Override
    public EvaluationModel getEvaluation(Long evaluatorId, Long orderId) {
        if (evaluatorId == null || orderId == null) {
            return null;
        }
        return evaluationDao.selectByOrderAndEvaluator(orderId, evaluatorId);
    }

    @Override
    public List<EvaluationModel> listReceivedEvaluations(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return evaluationDao.selectByTargetId(userId);
    }

    @Override
    public List<EvaluationModel> listGivenEvaluations(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return evaluationDao.selectByEvaluatorId(userId);
    }

    @Override
    public List<EvaluationModel> listByOrderId(Long orderId, Long requesterId) {
        if (orderId == null || requesterId == null) {
            return List.of();
        }
        OrderModel order = orderDao.selectByPrimaryKey(orderId);
        if (order == null) {
            return List.of();
        }
        IdleItemModel idleItem = idleItemDao.selectByPrimaryKey(order.getIdleId());
        Long buyerId = order.getUserId();
        Long sellerId = idleItem != null ? idleItem.getUserId() : null;
        if (!Objects.equals(requesterId, buyerId) && !Objects.equals(requesterId, sellerId)) {
            return List.of();
        }
        return evaluationDao.selectByOrderId(orderId);
    }

    private void refreshCreditScore(Long userId) {
        Double avgScore = evaluationDao.selectAverageScoreByTargetId(userId);
        int credit = DEFAULT_CREDIT;
        if (avgScore != null) {
            credit = (int) Math.round(avgScore * 20);
            credit = Math.max(0, Math.min(100, credit));
        }
        UserModel update = new UserModel();
        update.setId(userId);
        update.setCreditScore(credit);
        userDao.updateByPrimaryKeySelective(update);
    }
}
