package com.nomnom.backend.controller;

import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.model.OrderDraftRequest;
import com.nomnom.backend.model.OrderSummary;
import com.nomnom.backend.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/current")
    public ApiResponse<List<OrderSummary>> getCurrentOrders(@RequestParam(required = false) Long userId,
                                                            HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        Long currentUserId = (Long) request.getAttribute("userId");
        Long effectiveUserId = "MERCHANT".equals(roleCode) ? userId : currentUserId;
        return ApiResponse.ok(orderService.getCurrentOrders(effectiveUserId));
    }

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestBody OrderDraftRequest request, HttpServletRequest httpRequest) {
        request.setUserId((Long) httpRequest.getAttribute("userId"));
        return ApiResponse.ok(orderService.preview(request));
    }

    @PostMapping
    public ApiResponse<OrderSummary> create(@RequestBody OrderDraftRequest request, HttpServletRequest httpRequest) {
        request.setUserId((Long) httpRequest.getAttribute("userId"));
        return ApiResponse.ok("order created", orderService.createDraftOrder(request));
    }
}
