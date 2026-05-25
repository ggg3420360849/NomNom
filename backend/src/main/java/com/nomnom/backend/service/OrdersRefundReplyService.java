package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersRefundReplyEntity;
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

    private final OrdersRefundReplyMapper refundMapper;
    private final OrdersMapper ordersMapper;
    private final WalletService walletService;
    private final MessagesService messagesService;

    public OrdersRefundReplyService(OrdersRefundReplyMapper refundMapper,
                                    OrdersMapper ordersMapper,
                                    WalletService walletService,
                                    MessagesService messagesService) {
        this.refundMapper = refundMapper;
        this.ordersMapper = ordersMapper;
        this.walletService = walletService;
        this.messagesService = messagesService;
    }

    @Transactional
    public void submitRefund(Long ordersId, String refundCause) {
        if (ordersId == null) {
            throw new IllegalArgumentException("订单不能为空。");
        }
        OrdersRefundReplyEntity existing = refundMapper.selectOne(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .eq(OrdersRefundReplyEntity::getOrdersId, ordersId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("该订单已有退款申请。");
        }

        OrdersRefundReplyEntity entity = new OrdersRefundReplyEntity();
        entity.setOrdersId(ordersId.intValue());
        entity.setStatus(2);
        entity.setRefundCause(refundCause);
        entity.setCreateTime(LocalDateTime.now());
        refundMapper.insert(entity);

        OrdersEntity order = ordersMapper.selectById(ordersId);
        messagesService.createMessageForRole(ROLE_MERCHANT,
                "收到订单 " + resolveOrderCode(order, ordersId) + " 的退款申请，原因：" + normalizeCause(refundCause),
                MESSAGE_TYPE_ORDER);
    }

    public OrdersRefundReplyEntity getByOrder(Long ordersId) {
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
        if (entity.getStatus() != null && entity.getStatus() == 3) {
            throw new IllegalArgumentException("该退款申请已通过。");
        }
        if (entity.getStatus() != null && entity.getStatus() == 4) {
            throw new IllegalArgumentException("该退款申请已驳回。");
        }

        OrdersEntity order = entity.getOrdersId() != null ? ordersMapper.selectById(entity.getOrdersId()) : null;
        BigDecimal refundAmount = order != null && order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        if (order != null && order.getUserId() != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            walletService.refund(order.getUserId(), refundAmount, order.getCode());
            order.setStatus(5);
            ordersMapper.updateById(order);
            messagesService.createMessage(order.getUserId(),
                    "订单 " + resolveOrderCode(order, order.getId()) + " 的退款已通过，金额 ￥" + refundAmount + " 已退回钱包。",
                    MESSAGE_TYPE_WALLET);
        }
        entity.setStatus(3);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
    }

    @Transactional
    public void rejectRefund(Long id, String cause) {
        OrdersRefundReplyEntity entity = refundMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("退款申请不存在。");
        }
        if (entity.getStatus() != null && entity.getStatus() == 3) {
            throw new IllegalArgumentException("该退款申请已通过。");
        }
        if (entity.getStatus() != null && entity.getStatus() == 4) {
            throw new IllegalArgumentException("该退款申请已驳回。");
        }
        OrdersEntity order = entity.getOrdersId() != null ? ordersMapper.selectById(entity.getOrdersId()) : null;
        entity.setStatus(4);
        entity.setRejectRefundCause(cause);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
        if (order != null && order.getUserId() != null) {
            messagesService.createMessage(order.getUserId(),
                    "订单 " + resolveOrderCode(order, order.getId()) + " 的退款申请已驳回，原因：" + normalizeCause(cause),
                    MESSAGE_TYPE_ORDER);
        }
    }

    private String resolveOrderCode(OrdersEntity order, Long fallbackId) {
        if (order != null && order.getCode() != null && !order.getCode().trim().isEmpty()) {
            return order.getCode().trim();
        }
        return "#" + fallbackId;
    }

    private String normalizeCause(String cause) {
        return cause == null || cause.trim().isEmpty() ? "未填写" : cause.trim();
    }
}
