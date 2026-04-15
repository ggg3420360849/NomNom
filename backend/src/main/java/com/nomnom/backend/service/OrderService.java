package com.nomnom.backend.service;

import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersItemEntity;
import com.nomnom.backend.mapper.OrderReadMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.mapper.PackageReadMapper;
import com.nomnom.backend.model.OrderDraftRequest;
import com.nomnom.backend.model.OrderItemRequest;
import com.nomnom.backend.model.OrderSummary;
import com.nomnom.backend.model.PackageSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderReadMapper orderReadMapper;
    private final PackageReadMapper packageReadMapper;
    private final OrdersMapper ordersMapper;
    private final OrdersItemMapper ordersItemMapper;

    public OrderService(
            OrderReadMapper orderReadMapper,
            PackageReadMapper packageReadMapper,
            OrdersMapper ordersMapper,
            OrdersItemMapper ordersItemMapper
    ) {
        this.orderReadMapper = orderReadMapper;
        this.packageReadMapper = packageReadMapper;
        this.ordersMapper = ordersMapper;
        this.ordersItemMapper = ordersItemMapper;
    }

    public List<OrderSummary> getCurrentOrders(Long userId) {
        return orderReadMapper.selectCurrentOrders(userId);
    }

    public Map<String, Object> preview(OrderDraftRequest request) {
        ResolvedOrder resolvedOrder = resolveOrder(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerName", resolveCustomerName(request));
        payload.put("remark", normalizeText(request.getRemark()));
        payload.put("itemCount", resolvedOrder.itemCount);
        payload.put("totalAmount", resolvedOrder.totalAmount);
        payload.put("suggestion", "Connected to database. Preview includes item count, total amount, and order remark.");
        return payload;
    }

    @Transactional
    public OrderSummary createDraftOrder(OrderDraftRequest request) {
        ResolvedOrder resolvedOrder = resolveOrder(request);
        LocalDateTime now = LocalDateTime.now();

        OrdersEntity order = new OrdersEntity();
        order.setCode(generateOrderCode(now));
        order.setUserId(request.getUserId());
        order.setStatus(1);
        order.setTotalPrice(resolvedOrder.totalAmount);
        order.setCreateTime(now);
        ordersMapper.insert(order);

        for (ResolvedOrderLine line : resolvedOrder.lines) {
            OrdersItemEntity orderItem = new OrdersItemEntity();
            orderItem.setOrdersId(order.getId());
            orderItem.setDishesPackageId(line.snapshot.getPackageId());
            orderItem.setSnapPrice(line.snapshot.getPrice());
            orderItem.setSnapName(line.snapshot.resolveDisplayName());
            orderItem.setSnapCover(line.snapshot.getCoverUrl());
            orderItem.setBuyNumber(line.quantity);
            orderItem.setTotalPrice(line.lineTotal);
            orderItem.setCreateTime(now);
            ordersItemMapper.insert(orderItem);
        }

        return new OrderSummary(
                order.getCode(),
                resolveCustomerName(request),
                "PENDING_PAYMENT",
                resolvedOrder.totalAmount.doubleValue(),
                resolvedOrder.itemCount
        );
    }

    private ResolvedOrder resolveOrder(OrderDraftRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty.");
        }

        List<Long> packageIds = request.getItems().stream()
                .map(OrderItemRequest::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (packageIds.isEmpty()) {
            throw new IllegalArgumentException("Each order item must provide a package id.");
        }

        Map<Long, PackageSnapshot> snapshotMap = packageReadMapper.selectPackageSnapshots(packageIds).stream()
                .collect(Collectors.toMap(PackageSnapshot::getPackageId, Function.identity()));

        List<Long> missingIds = packageIds.stream()
                .filter(id -> !snapshotMap.containsKey(id))
                .collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Package ids not found: " + missingIds);
        }

        List<ResolvedOrderLine> lines = new ArrayList<>();
        BigDecimal totalAmount = ZERO;
        int itemCount = 0;

        for (OrderItemRequest requestItem : request.getItems()) {
            if (requestItem.getItemId() == null) {
                throw new IllegalArgumentException("Each order item must provide a package id.");
            }
            if (requestItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0.");
            }

            PackageSnapshot snapshot = snapshotMap.get(requestItem.getItemId());
            if (snapshot.getDishStatus() == null || snapshot.getDishStatus() != 1) {
                throw new IllegalArgumentException("Package " + requestItem.getItemId() + " is currently unavailable.");
            }

            BigDecimal lineTotal = snapshot.getPrice().multiply(BigDecimal.valueOf(requestItem.getQuantity()));
            lines.add(new ResolvedOrderLine(snapshot, requestItem.getQuantity(), lineTotal));
            totalAmount = totalAmount.add(lineTotal);
            itemCount += requestItem.getQuantity();
        }

        return new ResolvedOrder(lines, totalAmount, itemCount);
    }

    private String resolveCustomerName(OrderDraftRequest request) {
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            return request.getCustomerName().trim();
        }
        if (request.getUserId() != null) {
            return "User-" + request.getUserId();
        }
        return "Guest";
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateOrderCode(LocalDateTime now) {
        return "NN" + now.format(ORDER_CODE_TIME) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static final class ResolvedOrder {
        private final List<ResolvedOrderLine> lines;
        private final BigDecimal totalAmount;
        private final int itemCount;

        private ResolvedOrder(List<ResolvedOrderLine> lines, BigDecimal totalAmount, int itemCount) {
            this.lines = lines;
            this.totalAmount = totalAmount;
            this.itemCount = itemCount;
        }
    }

    private static final class ResolvedOrderLine {
        private final PackageSnapshot snapshot;
        private final int quantity;
        private final BigDecimal lineTotal;

        private ResolvedOrderLine(PackageSnapshot snapshot, int quantity, BigDecimal lineTotal) {
            this.snapshot = snapshot;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }
    }
}
