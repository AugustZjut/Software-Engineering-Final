package com.second.hand.trading.server.service;

import com.second.hand.trading.server.dao.EvaluationDao;
import com.second.hand.trading.server.dao.IdleItemDao;
import com.second.hand.trading.server.dao.OrderDao;
import com.second.hand.trading.server.dao.UserDao;
import com.second.hand.trading.server.model.EvaluationModel;
import com.second.hand.trading.server.model.IdleItemModel;
import com.second.hand.trading.server.model.OrderModel;
import com.second.hand.trading.server.model.UserModel;
import com.second.hand.trading.server.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    @Mock
    private EvaluationDao evaluationDao;

    @Mock
    private OrderDao orderDao;

    @Mock
    private IdleItemDao idleItemDao;

    @Mock
    private UserDao userDao;

    @Test
    void submitEvaluationReturnsPersistedModelAndUpdatesCredit() {
        OrderModel order = new OrderModel();
        order.setId(10L);
        order.setUserId(1L);
        order.setIdleId(100L);
        order.setOrderStatus((byte) 3);
        when(orderDao.selectByPrimaryKey(10L)).thenReturn(order);

        IdleItemModel idle = new IdleItemModel();
        idle.setId(100L);
        idle.setUserId(2L);
        when(idleItemDao.selectByPrimaryKey(100L)).thenReturn(idle);

        when(evaluationDao.insertSelective(any(EvaluationModel.class))).thenReturn(1);
        when(evaluationDao.selectAverageScoreByTargetId(2L)).thenReturn(4.5D);

        EvaluationModel stored = new EvaluationModel();
        stored.setId(99L);
        stored.setOrderId(10L);
        stored.setEvaluatorId(1L);
        stored.setTargetId(2L);
        stored.setScore(5);
        UserModel evaluator = new UserModel();
        evaluator.setId(1L);
        evaluator.setNickname("buyer");
        stored.setEvaluator(evaluator);
        when(evaluationDao.selectByOrderAndEvaluator(10L, 1L)).thenReturn(stored);

        EvaluationModel result = evaluationService.submitEvaluation(1L, 10L, 7, "Great");

        assertThat(result).isEqualTo(stored);
        ArgumentCaptor<EvaluationModel> evalCaptor = ArgumentCaptor.forClass(EvaluationModel.class);
        verify(evaluationDao).insertSelective(evalCaptor.capture());
        assertThat(evalCaptor.getValue().getScore()).isEqualTo(5);
        assertThat(evalCaptor.getValue().getTargetId()).isEqualTo(2L);

        ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        verify(userDao).updateByPrimaryKeySelective(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(userCaptor.getValue().getCreditScore()).isEqualTo(90);
    }

    @Test
    void submitEvaluationRejectsWhenUserNotParticipant() {
        OrderModel order = new OrderModel();
        order.setId(11L);
        order.setUserId(1L);
        order.setIdleId(200L);
        order.setOrderStatus((byte) 3);
        when(orderDao.selectByPrimaryKey(11L)).thenReturn(order);

        IdleItemModel idle = new IdleItemModel();
        idle.setId(200L);
        idle.setUserId(2L);
        when(idleItemDao.selectByPrimaryKey(200L)).thenReturn(idle);

        EvaluationModel result = evaluationService.submitEvaluation(3L, 11L, 4, "ok");

        assertThat(result).isNull();
        verify(evaluationDao, never()).insertSelective(any(EvaluationModel.class));
    }

    @Test
    void listGivenEvaluationsHandlesNullUser() {
        List<EvaluationModel> result = evaluationService.listGivenEvaluations(null);
        assertThat(result).isEqualTo(Collections.emptyList());
    }
    
    @Test
    void listByOrderIdDelegatesToDao() {
        OrderModel order = new OrderModel();
        order.setId(55L);
        order.setUserId(1L);
        order.setIdleId(200L);
        when(orderDao.selectByPrimaryKey(55L)).thenReturn(order);

        IdleItemModel idle = new IdleItemModel();
        idle.setId(200L);
        idle.setUserId(2L);
        when(idleItemDao.selectByPrimaryKey(200L)).thenReturn(idle);

        EvaluationModel record = new EvaluationModel();
        when(evaluationDao.selectByOrderId(55L)).thenReturn(List.of(record));

        List<EvaluationModel> result = evaluationService.listByOrderId(55L, 1L);

        assertThat(result).containsExactly(record);
        verify(evaluationDao, times(1)).selectByOrderId(55L);
    }

    @Test
    void listByOrderIdBlocksUnrelatedUser() {
        OrderModel order = new OrderModel();
        order.setId(77L);
        order.setUserId(1L);
        order.setIdleId(300L);
        when(orderDao.selectByPrimaryKey(77L)).thenReturn(order);

        IdleItemModel idle = new IdleItemModel();
        idle.setId(300L);
        idle.setUserId(2L);
        when(idleItemDao.selectByPrimaryKey(300L)).thenReturn(idle);

        List<EvaluationModel> result = evaluationService.listByOrderId(77L, 9L);

        assertThat(result).isEmpty();
        verify(evaluationDao, never()).selectByOrderId(77L);
    }
}
