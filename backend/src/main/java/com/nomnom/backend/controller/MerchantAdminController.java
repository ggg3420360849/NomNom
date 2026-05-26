package com.nomnom.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.common.ApiResponse;
import com.nomnom.backend.entity.*;
import com.nomnom.backend.mapper.*;
import com.nomnom.backend.model.AddDishRequest;
import com.nomnom.backend.model.DishDetailResponse;
import com.nomnom.backend.model.DashboardResponse;
import com.nomnom.backend.model.DishListResponse;
import com.nomnom.backend.model.EvaluationResponse;
import com.nomnom.backend.service.DishesEvaluationsService;
import com.nomnom.backend.service.MessagesService;
import com.nomnom.backend.service.OrdersRefundReplyService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class MerchantAdminController {
    private static final int TABLE_STATUS_IDLE = 0;
    private static final int TABLE_STATUS_SEATED = 1;
    private static final int TABLE_STATUS_DINING = 2;
    private static final int TABLE_STATUS_ORDERED = 3;
    private static final int TABLE_STATUS_PENDING = 4;
    private static final int TABLE_STATUS_BOOKED = 5;
    private static final int TABLE_STATUS_REPAIR = 6;

    private final DishesMapper dishesMapper;
    private final DishesTableMapper tableMapper;
    private final DishesPackageMapper packageMapper;
    private final DishesTypeMapper typeMapper;
    private final OrdersMapper ordersMapper;
    private final OrdersItemMapper ordersItemMapper;
    private final OrdersRefundReplyMapper refundReplyMapper;
    private final WalletInfoMapper walletInfoMapper;
    private final WalletMapper walletMapper;
    private final DishesEvaluationsMapper evaluationsMapper;
    private final ImagesMapper imagesMapper;
    private final ObjectMapper objectMapper;
    private final DishesEvaluationsService evaluationsService;
    private final MessagesService messagesService;
    private final OrdersRefundReplyService refundService;

    public MerchantAdminController(DishesMapper dishesMapper,
                                   DishesTableMapper tableMapper,
                                   DishesPackageMapper packageMapper,
                                   DishesTypeMapper typeMapper,
                                   OrdersMapper ordersMapper,
                                   OrdersItemMapper ordersItemMapper,
                                   OrdersRefundReplyMapper refundReplyMapper,
                                   WalletInfoMapper walletInfoMapper,
                                   WalletMapper walletMapper,
                                   DishesEvaluationsMapper evaluationsMapper,
                                   ImagesMapper imagesMapper,
                                   ObjectMapper objectMapper,
                                   DishesEvaluationsService evaluationsService,
                                   MessagesService messagesService,
                                   OrdersRefundReplyService refundService) {
        this.dishesMapper = dishesMapper;
        this.tableMapper = tableMapper;
        this.packageMapper = packageMapper;
        this.typeMapper = typeMapper;
        this.ordersMapper = ordersMapper;
        this.ordersItemMapper = ordersItemMapper;
        this.refundReplyMapper = refundReplyMapper;
        this.walletInfoMapper = walletInfoMapper;
        this.walletMapper = walletMapper;
        this.evaluationsMapper = evaluationsMapper;
        this.imagesMapper = imagesMapper;
        this.objectMapper = objectMapper;
        this.evaluationsService = evaluationsService;
        this.messagesService = messagesService;
        this.refundService = refundService;
    }

    // ==================== 菜品管理 ====================

    @GetMapping("/dishes")
    public ApiResponse<List<DishListResponse>> listDishes() {
        List<DishesEntity> dishes = dishesMapper.selectList(
                new LambdaQueryWrapper<DishesEntity>()
                        .orderByDesc(DishesEntity::getCreateTime));

        Map<Integer, String> typeMap = typeMapper.selectList(null).stream()
                .collect(Collectors.toMap(DishesTypeEntity::getId, t -> t.getName() != null ? t.getName() : ""));

        List<Long> dishIds = dishes.stream()
                .map(d -> d.getId() != null ? d.getId().longValue() : null)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        Map<Long, BigDecimal> priceMap = dishIds.isEmpty() ? Map.of() :
                packageMapper.selectList(new LambdaQueryWrapper<DishesPackageEntity>()
                                .in(DishesPackageEntity::getDishesId, dishIds))
                        .stream()
                        .collect(Collectors.groupingBy(DishesPackageEntity::getDishesId,
                                Collectors.collectingAndThen(
                                        Collectors.minBy(java.util.Comparator.comparing(DishesPackageEntity::getPrice)),
                                        opt -> opt.map(DishesPackageEntity::getPrice).orElse(BigDecimal.ZERO))));

        List<DishListResponse> result = dishes.stream().map(d -> {
            DishListResponse r = new DishListResponse();
            r.setId(d.getId());
            r.setTypeId(d.getTypeId());
            r.setTypeName(d.getTypeId() != null ? typeMap.getOrDefault(d.getTypeId(), "") : "");
            r.setName(d.getName());
            r.setDetail(d.getDetail());
            r.setCoverUrl(d.getCoverUrl());
            r.setStatus(d.getStatus());
            r.setStock(d.getStock());
            r.setPrice(priceMap.getOrDefault(d.getId() != null ? d.getId().longValue() : -1L, BigDecimal.ZERO));
            r.setMemberPrice(d.getMemberPrice());
            r.setSpecs(d.getSpecs());
            r.setIsRecommend(d.getIsRecommend() != null ? d.getIsRecommend() : 0);
            r.setRecommendIds(d.getRecommendIds());
            r.setCreateTime(d.getCreateTime());
            return r;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/dishes/{id}")
    public ApiResponse<DishDetailResponse> getDish(@PathVariable Integer id) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        List<DishesPackageEntity> packages = packageMapper.selectList(
                new LambdaQueryWrapper<DishesPackageEntity>()
                        .eq(DishesPackageEntity::getDishesId, id.longValue()));
        return ApiResponse.ok(toDetail(dish, packages));
    }

    @Transactional
    @PostMapping("/dishes/add")
    public ApiResponse<Void> addDish(@RequestBody AddDishRequest request) {
        DishesEntity dish = new DishesEntity();
        applyRequest(dish, request);
        dish.setCreateTime(new Date());
        dishesMapper.insert(dish);

        if (dish.getId() != null) {
            DishesPackageEntity pkg = new DishesPackageEntity();
            pkg.setDishesId(dish.getId().longValue());
            pkg.setName("标准份");
            pkg.setPrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO);
            pkg.setCreateTime(LocalDateTime.now());
            packageMapper.insert(pkg);
        }

        return ApiResponse.ok("新增成功", null);
    }

    @Transactional
    @PutMapping("/dishes/{id}")
    public ApiResponse<Void> updateDish(@PathVariable Integer id, @RequestBody AddDishRequest request) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        applyRequest(dish, request);
        dishesMapper.updateById(dish);

        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            List<DishesPackageEntity> packages = packageMapper.selectList(
                    new LambdaQueryWrapper<DishesPackageEntity>()
                            .eq(DishesPackageEntity::getDishesId, id.longValue())
                            .orderByAsc(DishesPackageEntity::getPrice)
                            .last("LIMIT 1"));
            if (!packages.isEmpty()) {
                DishesPackageEntity pkg = packages.get(0);
                pkg.setPrice(request.getPrice());
                packageMapper.updateById(pkg);
            }
        }
        return ApiResponse.ok("更新成功", null);
    }

    @PutMapping("/dishes/{id}/status")
    public ApiResponse<Void> toggleDishStatus(@PathVariable Integer id, @RequestParam Integer status) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        dish.setStatus(status);
        dishesMapper.updateById(dish);
        return ApiResponse.ok(status == 1 ? "已上架" : "已下架", null);
    }

    @Transactional
    @DeleteMapping("/dishes/{id}")
    public ApiResponse<Void> deleteDish(@PathVariable Integer id) {
        DishesEntity dish = dishesMapper.selectById(id);
        if (dish == null) {
            return ApiResponse.fail("菜品不存在");
        }
        packageMapper.delete(new LambdaQueryWrapper<DishesPackageEntity>()
                .eq(DishesPackageEntity::getDishesId, id.longValue()));
        dishesMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public ApiResponse<List<DishesTypeEntity>> listCategories(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DishesTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(DishesTypeEntity::getName, keyword.trim());
        }
        wrapper.orderByAsc(DishesTypeEntity::getId);
        return ApiResponse.ok(typeMapper.selectList(wrapper));
    }

    @PostMapping("/categories")
    public ApiResponse<Void> addCategory(@RequestBody DishesTypeEntity category) {
        String name = normalizeRequiredText(category != null ? category.getName() : null);
        if (name.isEmpty()) {
            return ApiResponse.fail("分类名称不能为空");
        }
        if (category == null) {
            category = new DishesTypeEntity();
        }
        category.setName(name);
        category.setIconUri(normalizeOptionalText(category.getIconUri()));
        typeMapper.insert(category);
        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Integer id, @RequestBody DishesTypeEntity category) {
        DishesTypeEntity existing = typeMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.fail("分类不存在");
        }
        String name = normalizeRequiredText(category != null ? category.getName() : null);
        if (name.isEmpty()) {
            return ApiResponse.fail("分类名称不能为空");
        }
        existing.setName(name);
        existing.setIconUri(normalizeOptionalText(category.getIconUri()));
        typeMapper.updateById(existing);
        return ApiResponse.ok("更新成功", null);
    }

    @Transactional
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Integer id) {
        if (typeMapper.selectById(id) == null) {
            return ApiResponse.fail("分类不存在");
        }
        typeMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 仪表盘 ====================

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        List<OrdersEntity> allOrders = ordersMapper.selectList(null);
        List<OrdersItemEntity> allItems = ordersItemMapper.selectList(null);

        DashboardResponse d = new DashboardResponse();
        d.setTotalOrders(allOrders.size());

        java.time.LocalDate today = java.time.LocalDate.now();
        java.math.BigDecimal todayRevenue = java.math.BigDecimal.ZERO;
        int todayCount = 0;
        int p1 = 0, p2 = 0, p3 = 0, p4 = 0;
        for (OrdersEntity o : allOrders) {
            if (o.getStatus() != null) {
                switch (o.getStatus()) {
                    case 1: p1++; break;
                    case 2: p2++; break;
                    case 3: p3++; break;
                    case 4: p4++; break;
                }
            }
            if (o.getCreateTime() != null && o.getCreateTime().toLocalDate().equals(today)) {
                todayCount++;
                todayRevenue = todayRevenue.add(o.getTotalPrice() != null ? o.getTotalPrice() : java.math.BigDecimal.ZERO);
            }
        }
        d.setPendingCount(p1);
        d.setPaidCount(p2);
        d.setPreparingCount(p3);
        d.setCompletedCount(p4);
        d.setTodayOrderCount(todayCount);
        d.setTodayRevenue(todayRevenue);

        java.util.Map<String, Integer> dishSales = new java.util.LinkedHashMap<>();
        for (OrdersItemEntity item : allItems) {
            String name = item.getSnapName() != null ? item.getSnapName() : "未知";
            dishSales.merge(name.split(" - ")[0], item.getBuyNumber() != null ? item.getBuyNumber() : 0, Integer::sum);
        }
        d.setTopDishes(dishSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> { java.util.Map<String, Object> m = new LinkedHashMap<>(); m.put("name", e.getKey()); m.put("sales", e.getValue()); return m; })
                .collect(java.util.stream.Collectors.toList()));

        return ApiResponse.ok(d);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public ApiResponse<List<OrdersEntity>> listOrders(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<OrdersEntity> wrapper = new LambdaQueryWrapper<OrdersEntity>()
                .orderByDesc(OrdersEntity::getCreateTime);
        if (status != null) {
            wrapper.eq(OrdersEntity::getStatus, status);
        }
        return ApiResponse.ok(ordersMapper.selectList(wrapper));
    }

    @PutMapping("/orders/{id}/status")
    public ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        OrdersEntity order = ordersMapper.selectById(id);
        if (order == null) {
            return ApiResponse.fail("订单不存在");
        }
        order.setStatus(status);
        ordersMapper.updateById(order);
        syncTableStatusWithOrder(order, status);
        if (order.getUserId() != null) {
            messagesService.createMessage(order.getUserId(),
                    "订单 " + order.getCode() + " 状态更新为：" + resolveOrderStatusLabel(status)
                            + (isCompletedOrderStatus(status) ? "，可在我的订单中评价菜品。" : ""),
                    2);
        }
        return ApiResponse.ok("状态更新成功", null);
    }

    @Transactional
    @DeleteMapping("/orders/invalid")
    public ApiResponse<Map<String, Object>> deleteInvalidOrders() {
        List<OrdersEntity> orders = ordersMapper.selectList(null);
        if (orders.isEmpty()) {
            return ApiResponse.ok(buildCleanupResult(List.of(), 0, 0, 0, 0));
        }

        List<OrdersItemEntity> items = ordersItemMapper.selectList(null);
        Map<Long, List<OrdersItemEntity>> itemsByOrder = items.stream()
                .filter(item -> item.getOrdersId() != null)
                .collect(Collectors.groupingBy(OrdersItemEntity::getOrdersId));

        List<Long> invalidOrderIds = orders.stream()
                .filter(order -> isInvalidOrder(order, itemsByOrder.get(order.getId())))
                .map(OrdersEntity::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (invalidOrderIds.isEmpty()) {
            return ApiResponse.ok(buildCleanupResult(invalidOrderIds, 0, 0, 0, 0));
        }

        List<Integer> invalidOrderIntIds = invalidOrderIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

        List<Integer> evaluationIds = evaluationsMapper.selectList(new LambdaQueryWrapper<DishesEvaluationsEntity>()
                        .in(DishesEvaluationsEntity::getOrdersId, invalidOrderIntIds))
                .stream()
                .map(DishesEvaluationsEntity::getId)
                .collect(Collectors.toList());
        int deletedImages = evaluationIds.isEmpty() ? 0 : imagesMapper.delete(new LambdaQueryWrapper<ImagesEntity>()
                .in(ImagesEntity::getDishesEvaluationsId, evaluationIds));
        int deletedEvaluations = evaluationsMapper.delete(new LambdaQueryWrapper<DishesEvaluationsEntity>()
                .in(DishesEvaluationsEntity::getOrdersId, invalidOrderIntIds));
        int deletedRefunds = refundReplyMapper.delete(new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                .in(OrdersRefundReplyEntity::getOrdersId, invalidOrderIntIds));
        int deletedItems = ordersItemMapper.delete(new LambdaQueryWrapper<OrdersItemEntity>()
                .in(OrdersItemEntity::getOrdersId, invalidOrderIds));
        int deletedOrders = ordersMapper.deleteBatchIds(invalidOrderIds);

        return ApiResponse.ok("异常订单已清理",
                buildCleanupResult(invalidOrderIds, deletedOrders, deletedItems, deletedEvaluations, deletedRefunds, deletedImages));
    }

    // ==================== 审核、对账与统计 ====================

    @GetMapping("/audits")
    public ApiResponse<List<Map<String, Object>>> listAudits() {
        List<Map<String, Object>> rows = new ArrayList<>();

        List<OrdersRefundReplyEntity> refunds = refundReplyMapper.selectList(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .orderByDesc(OrdersRefundReplyEntity::getCreateTime));
        for (OrdersRefundReplyEntity refund : refunds) {
            OrdersEntity order = refund.getOrdersId() != null ? ordersMapper.selectById(refund.getOrdersId()) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", refund.getId());
            row.put("type", "REFUND");
            row.put("desc", "订单 " + resolveOrderCode(order, refund.getOrdersId()) + " "
                    + resolveRefundRequestLabel(refund.getRefundCause()) + "原因："
                    + stripRefundRequestPrefix(normalizeDisplayText(refund.getRefundCause(), "未填写")));
            row.put("status", resolveRefundStatus(refund.getStatus()));
            row.put("rejectCause", refund.getRejectRefundCause());
            row.put("createTime", refund.getCreateTime());
            rows.add(row);
        }

        List<WalletInfoEntity> walletInfos = walletInfoMapper.selectList(
                new LambdaQueryWrapper<WalletInfoEntity>()
                        .orderByDesc(WalletInfoEntity::getCreateTime)
                        .last("LIMIT 50"));
        Map<Integer, WalletEntity> walletById = walletMapper.selectList(null).stream()
                .filter(wallet -> wallet.getId() != null)
                .collect(Collectors.toMap(WalletEntity::getId, wallet -> wallet, (a, b) -> a));
        for (WalletInfoEntity info : walletInfos) {
            if (!isRechargeFlow(info)) {
                continue;
            }
            WalletEntity wallet = walletById.get(info.getWalletId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "CHARGE-" + info.getId());
            row.put("type", "CHARGE");
            row.put("desc", "顾客 #" + (wallet != null ? wallet.getUserId() : "未知")
                    + " 充值 ￥" + normalizeAmount(info.getSurplusMoney()));
            row.put("status", "COMPLETED");
            row.put("createTime", info.getCreateTime());
            rows.add(row);
        }

        rows.sort((a, b) -> String.valueOf(b.getOrDefault("createTime", ""))
                .compareTo(String.valueOf(a.getOrDefault("createTime", ""))));
        return ApiResponse.ok(rows);
    }

    @PutMapping("/audit/{id}")
    public ApiResponse<Void> auditRegistrationOrRefund(@PathVariable String id,
                                                       @RequestParam Boolean approved,
                                                       @RequestParam(required = false) String cause) {
        Long refundId = parseRefundAuditId(id);
        if (refundId == null) {
            return ApiResponse.fail("充值记录无需审批");
        }
        if (Boolean.TRUE.equals(approved)) {
            refundService.approveRefund(refundId);
            return ApiResponse.ok("审批已通过", null);
        }
        refundService.rejectRefund(refundId, cause);
        return ApiResponse.ok("申请已驳回", null);
    }

    @GetMapping("/finance/reconciliation")
    public ApiResponse<List<Map<String, Object>>> financeReconciliation() {
        List<OrdersEntity> orders = ordersMapper.selectList(
                new LambdaQueryWrapper<OrdersEntity>()
                        .orderByDesc(OrdersEntity::getCreateTime));
        List<WalletInfoEntity> paymentFlows = walletInfoMapper.selectList(
                new LambdaQueryWrapper<WalletInfoEntity>()
                        .lt(WalletInfoEntity::getSurplusMoney, BigDecimal.ZERO));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (OrdersEntity order : orders) {
            BigDecimal orderAmount = normalizeAmount(order.getTotalPrice());
            BigDecimal flowAmount = resolvePaymentFlowAmount(order, paymentFlows);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getId());
            row.put("orderNo", order.getCode());
            row.put("orderAmt", orderAmount);
            row.put("flowAmt", flowAmount);
            row.put("result", orderAmount.compareTo(flowAmount) == 0 ? "平账" : "异常");
            rows.add(row);
        }
        return ApiResponse.ok(rows);
    }

    @GetMapping("/stats")
    public ApiResponse<List<Map<String, Object>>> salesStats() {
        Map<String, DishSalesStat> stats = new LinkedHashMap<>();
        for (OrdersItemEntity item : ordersItemMapper.selectList(null)) {
            String name = resolveDishName(item.getSnapName());
            DishSalesStat stat = stats.computeIfAbsent(name, DishSalesStat::new);
            int quantity = item.getBuyNumber() != null ? item.getBuyNumber() : 0;
            BigDecimal revenue = item.getTotalPrice() != null
                    ? item.getTotalPrice()
                    : normalizeAmount(item.getSnapPrice()).multiply(BigDecimal.valueOf(quantity));
            stat.add(quantity, revenue);
        }

        int[] rank = {1};
        List<Map<String, Object>> rows = stats.values().stream()
                .sorted(Comparator.comparingInt(DishSalesStat::getSales).reversed()
                        .thenComparing(DishSalesStat::getRevenue, Comparator.reverseOrder()))
                .map(stat -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rank", rank[0]++);
                    row.put("name", stat.getName());
                    row.put("sales", stat.getSales());
                    row.put("revenue", stat.getRevenue());
                    return row;
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    // ==================== 桌台管理 ====================

    @GetMapping("/tables")
    public ApiResponse<List<DishesTableEntity>> getTables() {
        List<DishesTableEntity> tables = tableMapper.selectList(
                new LambdaQueryWrapper<DishesTableEntity>()
                        .orderByAsc(DishesTableEntity::getArea)
                        .orderByAsc(DishesTableEntity::getNumber));
        return ApiResponse.ok("获取桌台成功", tables);
    }

    @CrossOrigin
    @GetMapping("/customer/tables")
    public ApiResponse<List<DishesTableEntity>> getTablesForCustomer() {
        List<DishesTableEntity> tables = tableMapper.selectList(
                new LambdaQueryWrapper<DishesTableEntity>()
                        .eq(DishesTableEntity::getStatus, TABLE_STATUS_IDLE)
                        .orderByAsc(DishesTableEntity::getArea)
                        .orderByAsc(DishesTableEntity::getNumber));
        return ApiResponse.ok("获取桌台成功", tables);
    }

    @PutMapping("/tables/status")
    public ApiResponse<Void> updateTableStatus(@RequestParam Integer tableId, @RequestParam Integer status) {
        if (!isValidTableStatus(status)) {
            return ApiResponse.fail("桌台状态不合法");
        }
        DishesTableEntity table = tableMapper.selectById(tableId);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        table.setStatus(status);
        if (shouldClearTableAmount(status)) {
            table.setAmount(BigDecimal.ZERO);
        }
        tableMapper.updateById(table);
        return ApiResponse.ok("桌台已设为" + resolveTableStatusLabel(status), null);
    }

    @PostMapping("/tables")
    public ApiResponse<Void> addTable(@RequestBody DishesTableEntity table) {
        if (table.getStatus() == null) {
            table.setStatus(TABLE_STATUS_IDLE);
        } else if (!isValidTableStatus(table.getStatus())) {
            return ApiResponse.fail("桌台状态不合法");
        }
        if (table.getAmount() == null) {
            table.setAmount(BigDecimal.ZERO);
        }
        table.setCreateTime(LocalDateTime.now());
        tableMapper.insert(table);
        return ApiResponse.ok("新增成功", null);
    }

    @PutMapping("/tables/{id}")
    public ApiResponse<Void> updateTable(@PathVariable Long id, @RequestBody DishesTableEntity request) {
        DishesTableEntity table = tableMapper.selectById(id);
        if (table == null) {
            return ApiResponse.fail("桌台不存在");
        }
        table.setNumber(request.getNumber());
        table.setPersonNumber(request.getPersonNumber());
        table.setArea(request.getArea());
        table.setMinSpend(request.getMinSpend());
        if (request.getStatus() != null) {
            if (!isValidTableStatus(request.getStatus())) {
                return ApiResponse.fail("桌台状态不合法");
            }
            table.setStatus(request.getStatus());
            if (shouldClearTableAmount(request.getStatus())) {
                table.setAmount(BigDecimal.ZERO);
            }
        }
        tableMapper.updateById(table);
        return ApiResponse.ok("更新成功", null);
    }

    @DeleteMapping("/tables/{id}")
    public ApiResponse<Void> deleteTable(@PathVariable Long id) {
        if (tableMapper.selectById(id) == null) {
            return ApiResponse.fail("桌台不存在");
        }
        tableMapper.deleteById(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 评价管理 ====================

    @GetMapping("/reviews")
    public ApiResponse<List<EvaluationResponse>> listReviews() {
        return ApiResponse.ok(evaluationsService.getAllEvaluations());
    }

    @PostMapping("/reviews/{id}/reply")
    public ApiResponse<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reply = body != null ? body.get("reply") : null;
        if (reply == null || reply.trim().isEmpty()) {
            return ApiResponse.fail("回复内容不能为空");
        }
        evaluationsService.replyByMerchant(id, reply.trim());
        return ApiResponse.ok("回复成功", null);
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        evaluationsService.deleteEvaluation(id);
        return ApiResponse.ok("评价已删除", null);
    }

    // ==================== 内部方法 ====================

    private boolean isInvalidOrder(OrdersEntity order, List<OrdersItemEntity> items) {
        if (order == null) {
            return true;
        }
        if (order.getCode() == null || order.getCode().trim().isEmpty()) {
            return true;
        }
        if (order.getUserId() == null) {
            return true;
        }
        if (order.getStatus() == null || order.getStatus() < 1 || order.getStatus() > 5) {
            return true;
        }
        if (order.getTotalPrice() == null || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        if (items == null || items.isEmpty()) {
            return true;
        }
        return items.stream().anyMatch(item ->
                item.getDishesPackageId() == null
                        || item.getBuyNumber() == null
                        || item.getBuyNumber() <= 0
                        || item.getSnapName() == null
                        || item.getSnapName().trim().isEmpty()
                        || item.getSnapPrice() == null
                        || item.getSnapPrice().compareTo(BigDecimal.ZERO) <= 0
                        || item.getTotalPrice() == null
                        || item.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0);
    }

    private Map<String, Object> buildCleanupResult(List<Long> invalidOrderIds,
                                                   int deletedOrders,
                                                   int deletedItems,
                                                   int deletedEvaluations,
                                                   int deletedRefunds) {
        return buildCleanupResult(invalidOrderIds, deletedOrders, deletedItems, deletedEvaluations, deletedRefunds, 0);
    }

    private Map<String, Object> buildCleanupResult(List<Long> invalidOrderIds,
                                                   int deletedOrders,
                                                   int deletedItems,
                                                   int deletedEvaluations,
                                                   int deletedRefunds,
                                                   int deletedImages) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invalidOrderIds", invalidOrderIds);
        result.put("deletedOrders", deletedOrders);
        result.put("deletedItems", deletedItems);
        result.put("deletedEvaluations", deletedEvaluations);
        result.put("deletedRefunds", deletedRefunds);
        result.put("deletedImages", deletedImages);
        return result;
    }

    private String resolveRefundStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
            case 2:
                return "PENDING";
            case 3:
                return "APPROVED";
            case 4:
                return "REJECTED";
            case 1:
                return "NONE";
            default:
                return "UNKNOWN";
        }
    }

    private Long parseRefundAuditId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("REFUND-")) {
            value = value.substring("REFUND-".length());
        }
        if (!value.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(value);
    }

    private String resolveOrderCode(OrdersEntity order, Integer fallbackId) {
        return resolveOrderCode(order, fallbackId != null ? fallbackId.longValue() : null);
    }

    private String resolveOrderCode(OrdersEntity order, Long fallbackId) {
        if (order != null && order.getCode() != null && !order.getCode().trim().isEmpty()) {
            return order.getCode().trim();
        }
        return fallbackId != null ? "#" + fallbackId : "#未知";
    }

    private boolean isRechargeFlow(WalletInfoEntity info) {
        return info != null
                && info.getDetail() != null
                && info.getDetail().contains("充值")
                && normalizeAmount(info.getSurplusMoney()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private BigDecimal resolvePaymentFlowAmount(OrdersEntity order, List<WalletInfoEntity> paymentFlows) {
        if (order == null || order.getCode() == null || order.getCode().trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String code = order.getCode().trim();
        return paymentFlows.stream()
                .filter(flow -> flow.getDetail() != null && flow.getDetail().contains(code))
                .map(flow -> normalizeAmount(flow.getSurplusMoney()).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeDisplayText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String resolveRefundRequestLabel(String refundCause) {
        return refundCause != null && refundCause.startsWith("取消订单：") ? "取消订单" : "退款";
    }

    private String stripRefundRequestPrefix(String refundCause) {
        if (refundCause == null) {
            return "";
        }
        if (refundCause.startsWith("取消订单：")) {
            return refundCause.substring("取消订单：".length());
        }
        if (refundCause.startsWith("退款：")) {
            return refundCause.substring("退款：".length());
        }
        return refundCause;
    }

    private String resolveDishName(String snapName) {
        String value = normalizeDisplayText(snapName, "未知菜品");
        int packageIndex = value.indexOf(" - ");
        if (packageIndex > 0) {
            return value.substring(0, packageIndex);
        }
        int specIndex = value.indexOf(" / ");
        if (specIndex > 0) {
            return value.substring(0, specIndex);
        }
        return value;
    }

    private String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveOrderStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "待支付";
            case 2:
                return "已支付";
            case 3:
                return "制作中";
            case 4:
                return "已完成";
            case 5:
                return "已取消";
            default:
                return String.valueOf(status);
        }
    }

    private boolean isCompletedOrderStatus(Integer status) {
        return status != null && status == 4;
    }

    private boolean isValidTableStatus(Integer status) {
        return status != null && status >= TABLE_STATUS_IDLE && status <= TABLE_STATUS_REPAIR;
    }

    private boolean shouldClearTableAmount(Integer status) {
        return status != null && (status == TABLE_STATUS_IDLE
                || status == TABLE_STATUS_BOOKED
                || status == TABLE_STATUS_REPAIR);
    }

    private String resolveTableStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case TABLE_STATUS_IDLE:
                return "空闲";
            case TABLE_STATUS_SEATED:
                return "已入座";
            case TABLE_STATUS_DINING:
                return "就餐中";
            case TABLE_STATUS_ORDERED:
                return "已下单";
            case TABLE_STATUS_PENDING:
                return "待结账";
            case TABLE_STATUS_BOOKED:
                return "预订中";
            case TABLE_STATUS_REPAIR:
                return "维修中";
            default:
                return "未知";
        }
    }

    private void syncTableStatusWithOrder(OrdersEntity order, Integer orderStatus) {
        if (order == null || order.getTableId() == null || orderStatus == null) {
            return;
        }
        DishesTableEntity table = tableMapper.selectById(order.getTableId());
        if (table == null || (table.getStatus() != null && table.getStatus() == TABLE_STATUS_REPAIR)) {
            return;
        }
        if (orderStatus == 2) {
            table.setStatus(TABLE_STATUS_ORDERED);
        } else if (orderStatus == 3) {
            table.setStatus(TABLE_STATUS_DINING);
        } else if (orderStatus == 4 || orderStatus == 5) {
            table.setStatus(TABLE_STATUS_IDLE);
            table.setAmount(BigDecimal.ZERO);
        }
        tableMapper.updateById(table);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyRequest(DishesEntity dish, AddDishRequest request) {
        dish.setTypeId(request.getTypeId());
        dish.setName(request.getName());
        dish.setDetail(request.getDetail());
        dish.setCoverUrl(request.getCoverUrl());
        dish.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dish.setStock(request.getStock() != null ? request.getStock() : 999);
        dish.setMemberPrice(request.getMemberPrice());
        dish.setSpecs(buildSpecsJson(request));
        dish.setIsRecommend(request.getIsRecommend() != null ? request.getIsRecommend() : 0);
        dish.setRecommendIds(normalizeOptionalText(request.getRecommendIds()));
    }

    private String buildSpecsJson(AddDishRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (request.getSpecs() != null && !request.getSpecs().isEmpty()) {
            map.put("sizes", request.getSpecs());
        }
        if (request.getTastes() != null && !request.getTastes().isEmpty()) {
            map.put("spiciness", request.getTastes());
        }
        try {
            return map.isEmpty() ? null : objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private DishDetailResponse toDetail(DishesEntity dish, List<DishesPackageEntity> packages) {
        DishDetailResponse d = new DishDetailResponse();
        d.setId(dish.getId());
        d.setTypeId(dish.getTypeId());
        d.setName(dish.getName());
        d.setDetail(dish.getDetail());
        d.setCoverUrl(dish.getCoverUrl());
        d.setStatus(dish.getStatus());
        d.setStock(dish.getStock());
        d.setMemberPrice(dish.getMemberPrice());
        d.setSpecs(dish.getSpecs());
        d.setIsRecommend(dish.getIsRecommend() != null ? dish.getIsRecommend() : 0);
        d.setRecommendIds(dish.getRecommendIds());
        d.setCreateTime(dish.getCreateTime());
        d.setPackages(packages);
        return d;
    }

    private static final class DishSalesStat {
        private final String name;
        private int sales;
        private BigDecimal revenue = BigDecimal.ZERO;

        private DishSalesStat(String name) {
            this.name = name;
        }

        private void add(int quantity, BigDecimal amount) {
            this.sales += quantity;
            this.revenue = this.revenue.add(amount != null ? amount : BigDecimal.ZERO);
        }

        private String getName() {
            return name;
        }

        private int getSales() {
            return sales;
        }

        private BigDecimal getRevenue() {
            return revenue;
        }
    }
}
