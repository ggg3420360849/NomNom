package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.OrdersRefundReplyEntity;
import com.nomnom.backend.model.RefundRequest;
import com.nomnom.backend.service.OrdersRefundReplyService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final OrdersRefundReplyService refundService;

    public RefundController(OrdersRefundReplyService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ApiResponse<Void> submit(@RequestBody RefundRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.fail("请先登录");
        }
        refundService.submitRefund(request.getOrdersId(), request.getRefundCause());
        return ApiResponse.ok("退款申请已提交", null);
    }

    @GetMapping("/order/{ordersId}")
    public ApiResponse<OrdersRefundReplyEntity> getByOrder(@PathVariable Long ordersId) {
        return ApiResponse.ok(refundService.getByOrder(ordersId));
    }

    @GetMapping
    public ApiResponse<List<OrdersRefundReplyEntity>> listAll(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (!"MERCHANT".equals(roleCode)) {
            return ApiResponse.fail("无权访问");
        }
        return ApiResponse.ok(refundService.listAll());
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (!"MERCHANT".equals(roleCode)) {
            return ApiResponse.fail("无权操作");
        }
        refundService.approveRefund(id);
        return ApiResponse.ok("退款已通过", null);
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam String cause, HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (!"MERCHANT".equals(roleCode)) {
            return ApiResponse.fail("无权操作");
        }
        refundService.rejectRefund(id, cause);
        return ApiResponse.ok("已拒绝退款", null);
    }
}
