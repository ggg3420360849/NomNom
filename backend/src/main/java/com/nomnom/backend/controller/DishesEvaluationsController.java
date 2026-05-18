package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.EvaluationRequest;
import com.nomnom.backend.model.EvaluationResponse;
import com.nomnom.backend.service.DishesEvaluationsService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class DishesEvaluationsController {

    private final DishesEvaluationsService evaluationsService;

    public DishesEvaluationsController(DishesEvaluationsService evaluationsService) {
        this.evaluationsService = evaluationsService;
    }

    @GetMapping("/dish/{dishesId}")
    public ApiResponse<List<EvaluationResponse>> getDishEvaluations(@PathVariable Long dishesId) {
        return ApiResponse.ok(evaluationsService.getDishEvaluations(dishesId));
    }

    @GetMapping("/my")
    public ApiResponse<List<EvaluationResponse>> getMyEvaluations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(evaluationsService.getUserEvaluations(userId));
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody EvaluationRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("请先登录");
        }
        evaluationsService.createEvaluation(userId, request);
        return ApiResponse.ok("评价成功", null);
    }

    @PutMapping("/{id}/reply")
    public ApiResponse<Void> reply(@PathVariable Long id, @RequestParam String content) {
        evaluationsService.replyByMerchant(id, content);
        return ApiResponse.ok("回复成功", null);
    }
}
