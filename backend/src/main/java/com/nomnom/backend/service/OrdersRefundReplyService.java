package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.OrdersRefundReplyEntity;
import com.nomnom.backend.mapper.OrdersRefundReplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdersRefundReplyService {

    private final OrdersRefundReplyMapper refundMapper;

    public OrdersRefundReplyService(OrdersRefundReplyMapper refundMapper) {
        this.refundMapper = refundMapper;
    }

    @Transactional
    public void submitRefund(Long ordersId, String refundCause) {
        OrdersRefundReplyEntity existing = refundMapper.selectOne(
                new LambdaQueryWrapper<OrdersRefundReplyEntity>()
                        .eq(OrdersRefundReplyEntity::getOrdersId, ordersId.intValue())
                        .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("该订单已有退款申请");
        }

        OrdersRefundReplyEntity entity = new OrdersRefundReplyEntity();
        entity.setOrdersId(ordersId.intValue());
        entity.setStatus(2);
        entity.setRefundCause(refundCause);
        entity.setCreateTime(LocalDateTime.now());
        refundMapper.insert(entity);
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
            throw new IllegalArgumentException("退款申请不存在");
        }
        entity.setStatus(3);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
    }

    @Transactional
    public void rejectRefund(Long id, String cause) {
        OrdersRefundReplyEntity entity = refundMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("退款申请不存在");
        }
        entity.setStatus(4);
        entity.setRejectRefundCause(cause);
        entity.setRefunTime(LocalDateTime.now());
        refundMapper.updateById(entity);
    }
}
