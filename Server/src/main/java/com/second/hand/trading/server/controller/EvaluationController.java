package com.second.hand.trading.server.controller;

import com.second.hand.trading.server.enums.ErrorMsg;
import com.second.hand.trading.server.model.EvaluationModel;
import com.second.hand.trading.server.service.EvaluationService;
import com.second.hand.trading.server.vo.EvaluationCreateRequest;
import com.second.hand.trading.server.vo.ResultVo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    @Resource
    private EvaluationService evaluationService;

    @PostMapping("/add")
    public ResultVo<EvaluationModel> addEvaluation(@CookieValue("shUserId")
                                                   @NotNull(message = "登录异常 请重新登录")
                                                   @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                                   @RequestBody @Valid EvaluationCreateRequest request) {
        Long evaluatorId = Long.valueOf(shUserId);
        EvaluationModel existed = evaluationService.getEvaluation(evaluatorId, request.getOrderId());
        if (existed != null) {
            return ResultVo.fail(ErrorMsg.REPEAT_COMMIT_ERROR);
        }
        EvaluationModel created = evaluationService.submitEvaluation(evaluatorId,
                request.getOrderId(),
                request.getScore(),
                request.getContent());
        if (created == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        return ResultVo.success(created);
    }

    @GetMapping("/received")
    public ResultVo<List<EvaluationModel>> listReceived(@CookieValue("shUserId")
                                                        @NotNull(message = "登录异常 请重新登录")
                                                        @NotEmpty(message = "登录异常 请重新登录") String shUserId) {
        Long userId = Long.valueOf(shUserId);
        return ResultVo.success(evaluationService.listReceivedEvaluations(userId));
    }

    @GetMapping("/given")
    public ResultVo<List<EvaluationModel>> listGiven(@CookieValue("shUserId")
                                                     @NotNull(message = "登录异常 请重新登录")
                                                     @NotEmpty(message = "登录异常 请重新登录") String shUserId) {
        Long userId = Long.valueOf(shUserId);
        return ResultVo.success(evaluationService.listGivenEvaluations(userId));
    }

    @GetMapping("/order")
    public ResultVo<EvaluationModel> getOrderEvaluation(@CookieValue("shUserId")
                                                        @NotNull(message = "登录异常 请重新登录")
                                                        @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                                        @RequestParam Long orderId) {
        Long evaluatorId = Long.valueOf(shUserId);
        return ResultVo.success(evaluationService.getEvaluation(evaluatorId, orderId));
    }

    @GetMapping("/order/all")
    public ResultVo<List<EvaluationModel>> listOrderEvaluations(@CookieValue("shUserId")
                                                                @NotNull(message = "登录异常 请重新登录")
                                                                @NotEmpty(message = "登录异常 请重新登录") String shUserId,
                                                                @RequestParam Long orderId) {
        Long userId = Long.valueOf(shUserId);
        EvaluationModel self = evaluationService.getEvaluation(userId, orderId);
        List<EvaluationModel> evaluations = evaluationService.listByOrderId(orderId, userId);
        // Ensure latest self evaluation appears in the list if service returns stale data (cache safety).
        if (self != null && evaluations.stream().noneMatch(item -> item.getId() != null && item.getId().equals(self.getId()))) {
            evaluations = new ArrayList<>(evaluations);
            evaluations.add(self);
        }
        return ResultVo.success(evaluations);
    }
}
