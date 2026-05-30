package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersRefundReplyEntity;
import com.nomnom.backend.entity.DishesTableEntity;
import com.nomnom.backend.mapper.DishesTableMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.mapper.OrdersRefundReplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdersRefundReplyService {

    private static final int ROLE_MERCHANT = 2;
    private static final int MESSAGE_TYPE_ORDER = 2;
    private static final int MESSAGE_TYPE_WALLET = 3;
    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_PREPARING = 3;
    private static final int ORDER_STATUS_COMPLETED = 4;
    private static final int ORDER_STATUS_CANCELED = 5;
    private static final List<Integer> ACTIVE_ORDER_STATUSES = List.of(1, ORDER_STATUS_PAID, ORDER_STATUS_PREPARING);
    private static final int REFUND_STATUS_PENDING = 2;
    private static final int REFUND_STATUS_APPROVED = 3;
    private static final int REFUND_STATUS_REJECTED = 4;
    private static final int TABLE_STATUS_IDLE = 0;
    private static final int TABLE_STATUS_SEATED = 1;
    private static final int TABLE_STATUS_REPAIR = 6;
    private static final String REQUEST_TYPE_CANCEL = "CANCEL";
    private static final String REQUEST_TYPE_REFUND = "REFUND";

    private final OrdersRefundReplyMapper refundMapper;
    private final OrdersMapper ordersMapper;
    private final DishesTableMapper tableMapper;
    private final WalletService walletService;
    private final MessagesService messagesService;

    public OrdersRefundReplyService(OrdersRefundReplyMapper refundMapper,
                                    OrdersMapper ordersMapper,
                                    DishesTableMapper tableMapper,
                                    WalletService walletService,
                                    MessagesService messagesService) {
        this.refundMapper = refundMapper;
        this.ordersMapper = ordersMapper;
        this.tableMapper = tableMapper;
        this.walletService = walletService;
        this.messagesService = messagesService;
    }

    @Transactional
    public void submitRefund(Long userId, Long ordersId, String refundCause, String requestType) {
        if (ordersId == null) {
            throw new IllegalArgumentException("订单不能为空。");
        }
        if (userId == null) {
            throw new IllegalArgumentException("请先登录。");
        }
        String normalizedCause = normalizeRequiredCause(refundCause);
        String normalizedType = normalizeRequestType(requestType);

        OrdersEntity order = ordersMapper.selectById(ordersId);
        if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在或无权操作。");
        }
        validateOrderCanRequest(order, normalizedType);

        OrdersRefundReplyEntity existing = refundMapper.selectOne(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .eq(OrdersRefundReplyEntity::getOrdersId, ordersId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("该订单已有售后申请，无法重复提交。");
        }

        OrdersRefundReplyEntity entity = new OrdersRefundReplyEntity();
        entity.setOrdersId(ordersId.intValue());
        entity.setStatus(REFUND_STATUS_PENDING);
        entity.setRefundCause(buildStoredCause(normalizedType, normalizedCause));
        entity.setCreateTime(LocalDateTime.now());
        refundMapper.insert(entity);

        messagesService.createMessageForRole(ROLE_MERCHANT,
                "收到订单 " + resolveOrderCode(order, ordersId) + " 的" + resolveRequestLabel(normalizedType)
                        + "申请，原因：" + normalizedCause,
                MESSAGE_TYPE_ORDER);
    }

    public OrdersRefundReplyEntity getByOrder(Long userId, String roleCode, Long ordersId) {
        if (ordersId == null) {
            throw new IllegalArgumentException("订单不能为空。");
        }
        if (!"MERCHANT".equals(roleCode)) {
            OrdersEntity order = ordersMapper.selectById(ordersId);
            if (order == null || order.getUserId() == null || !order.getUserId().equals(userId)) {
                throw new IllegalArgumentException("订单不存在或无权查看。");
            }
        }
        return refundMapper.selectOne(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .eq(OrdersRefundReplyEntity::getOrdersId, ordersId.intValue())
                        .last("LIMIT 1"));
    }

    public List<OrdersRefundReplyEntity> listAll() {
        return refundMapper.selectList(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .orderByDesc(OrdersRefundReplyEntity::getCreateTime));
    }

    @Transactional
    public void approveRefund(Long id) {
        OrdersRefundReplyEntity entity = refundMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("退款申请不存在。");
        }
        if (entity.getStatus() != null && entity.getStatus() == REFUND_STATUS_APPROVED) {
            throw new IllegalArgumentException("该退款申请已通过。");
        }
        if (entity.getStatus() != null && entity.getStatus() == REFUND_STATUS_REJECTED) {
            throw new IllegalArgumentException("该退款申请已驳回。");
        }

        OrdersEntity order = entity.getOrdersId() != null ? ordersMapper.selectById(entity.getOrdersId()) : null;
        BigDecimal refundAmount = order != null && order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        if (order != null && order.getUserId() != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.refund(order.getUserId(), refundAmount, order.getCode());
            order.setStatus(ORDER_STATUS_CANCELED);
            ordersMapper.updateById(order);
            syncTableAfterOrderChange(order);
            messagesService.createMessage(order.getUserId(),
                    "订单 " + resolveOrderCode(order, order.getId()) + " 的" + resolveStoredRequestLabel(entity.getRefundCause())
                            + "申请已通过，金额 ￥" + refundAmount + " 已退回钱包。",
                    MESSAGE_TYPE_WALLET);
        }
        entity.setStatus(REFUND_STATUS_APPROVED);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
    }

    @Transactional
    public void rejectRefund(Long id, String cause) {
        OrdersRefundReplyEntity entity = refundMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("退款申请不存在。");
        }
        String normalizedCause = normalizeRequiredRejectCause(cause);
        if (entity.getStatus() != null && entity.getStatus() == REFUND_STATUS_APPROVED) {
            throw new IllegalArgumentException("该退款申请已通过。");
        }
        if (entity.getStatus() != null && entity.getStatus() == REFUND_STATUS_REJECTED) {
            throw new IllegalArgumentException("该退款申请已驳回。");
        }
        OrdersEntity order = entity.getOrdersId() != null ? ordersMapper.selectById(entity.getOrdersId()) : null;
        entity.setStatus(REFUND_STATUS_REJECTED);
        entity.setRejectRefundCause(normalizedCause);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
        if (order != null && order.getUserId() != null) {
            messagesService.createMessage(order.getUserId(),
                    "订单 " + resolveOrderCode(order, order.getId()) + " 的" + resolveStoredRequestLabel(entity.getRefundCause())
                            + "申请已驳回，原因：" + normalizedCause,
                    MESSAGE_TYPE_ORDER);
        }
    }

    private void validateOrderCanRequest(OrdersEntity order, String requestType) {
        Integer status = order.getStatus();
        if (status == null) {
            throw new IllegalArgumentException("订单状态异常，无法提交申请。");
        }
        if (status == ORDER_STATUS_CANCELED) {
            throw new IllegalArgumentException("订单已取消，无法提交申请。");
        }
        if (REQUEST_TYPE_CANCEL.equals(requestType)
                && status != ORDER_STATUS_PAID
                && status != ORDER_STATUS_PREPARING) {
            throw new IllegalArgumentException("只有已支付或制作中的订单可以申请取消。");
        }
        if (REQUEST_TYPE_REFUND.equals(requestType) && status != ORDER_STATUS_COMPLETED) {
            throw new IllegalArgumentException("只有已完成订单可以申请退款。");
        }
    }

    private void syncTableAfterOrderChange(OrdersEntity order) {
        DishesTableEntity table = resolveOrderTable(order);
        if (table == null || (table.getStatus() != null && table.getStatus() == TABLE_STATUS_REPAIR)) {
            return;
        }

        List<OrdersEntity> activeOrders = selectActiveOrdersForTable(table);
        if (activeOrders.isEmpty()) {
            table.setStatus(TABLE_STATUS_IDLE);
            table.setAmount(BigDecimal.ZERO);
            table.setOccupiedBy(null);
            tableMapper.updateById(table);
            return;
        }

        BigDecimal activeAmount = activeOrders.stream()
                .map(OrdersEntity::getTotalPrice)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        table.setStatus(TABLE_STATUS_SEATED);
        table.setAmount(activeAmount);
        table.setOccupiedBy(activeOrders.get(0).getUserId());
        tableMapper.updateById(table);
    }

    private DishesTableEntity resolveOrderTable(OrdersEntity order) {
        if (order == null) {
            return null;
        }
        if (order.getTableId() != null) {
            DishesTableEntity table = tableMapper.selectById(order.getTableId());
            if (table != null) {
                return table;
            }
        }
        String tableNumber = normalizeOptionalText(order.getTableNumber());
        if (tableNumber == null) {
            return null;
        }
        return tableMapper.selectOne(new LambdaQueryWrapper<DishesTableEntity>()
                .eq(DishesTableEntity::getNumber, tableNumber)
                .last("LIMIT 1"));
    }

    private List<OrdersEntity> selectActiveOrdersForTable(DishesTableEntity table) {
        LambdaQueryWrapper<OrdersEntity> wrapper = new LambdaQueryWrapper<OrdersEntity>()
                .in(OrdersEntity::getStatus, ACTIVE_ORDER_STATUSES);
        String tableNumber = normalizeOptionalText(table.getNumber());
        if (table.getId() != null && tableNumber != null) {
            wrapper.and(q -> q.eq(OrdersEntity::getTableId, table.getId())
                    .or()
                    .eq(OrdersEntity::getTableNumber, tableNumber));
        } else if (table.getId() != null) {
            wrapper.eq(OrdersEntity::getTableId, table.getId());
        } else if (tableNumber != null) {
            wrapper.eq(OrdersEntity::getTableNumber, tableNumber);
        } else {
            return List.of();
        }
        return ordersMapper.selectList(wrapper);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveOrderCode(OrdersEntity order, Long fallbackId) {
        if (order != null && order.getCode() != null && !order.getCode().trim().isEmpty()) {
            return order.getCode().trim();
        }
        return "#" + fallbackId;
    }

    private String normalizeRequiredCause(String cause) {
        if (cause == null || cause.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写申请原因。");
        }
        return cause.trim();
    }

    private String normalizeRequiredRejectCause(String cause) {
        if (cause == null || cause.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写驳回理由。");
        }
        return cause.trim();
    }

    private String normalizeRequestType(String requestType) {
        if (REQUEST_TYPE_CANCEL.equalsIgnoreCase(requestType)) {
            return REQUEST_TYPE_CANCEL;
        }
        return REQUEST_TYPE_REFUND;
    }

    private String resolveRequestLabel(String requestType) {
        return REQUEST_TYPE_CANCEL.equals(requestType) ? "取消订单" : "退款";
    }

    private String buildStoredCause(String requestType, String cause) {
        return resolveRequestLabel(requestType) + "：" + cause;
    }

    private String resolveStoredRequestLabel(String storedCause) {
        if (storedCause != null && storedCause.startsWith("取消订单：")) {
            return "取消订单";
        }
        return "退款";
    }
}
