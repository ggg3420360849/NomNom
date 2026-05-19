package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.DishesEvaluationsEntity;
import com.nomnom.backend.entity.ImagesEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.mapper.DishesEvaluationsMapper;
import com.nomnom.backend.mapper.EvaluationReadMapper;
import com.nomnom.backend.mapper.ImagesMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.model.EvaluationRequest;
import com.nomnom.backend.model.EvaluationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishesEvaluationsService {

    private final DishesEvaluationsMapper evaluationsMapper;
    private final EvaluationReadMapper evaluationReadMapper;
    private final ImagesMapper imagesMapper;
    private final OrdersMapper ordersMapper;

    public DishesEvaluationsService(DishesEvaluationsMapper evaluationsMapper,
                                    EvaluationReadMapper evaluationReadMapper,
                                    ImagesMapper imagesMapper,
                                    OrdersMapper ordersMapper) {
        this.evaluationsMapper = evaluationsMapper;
        this.evaluationReadMapper = evaluationReadMapper;
        this.imagesMapper = imagesMapper;
        this.ordersMapper = ordersMapper;
    }

    public List<EvaluationResponse> getDishEvaluations(Long dishesId) {
        List<EvaluationResponse> list = evaluationReadMapper.selectDishEvaluations(dishesId);
        attachImages(list);
        return list;
    }

    public List<EvaluationResponse> getUserEvaluations(Long userId) {
        List<EvaluationResponse> list = evaluationReadMapper.selectUserEvaluations(userId);
        attachImages(list);
        return list;
    }

    @Transactional
    public void createEvaluation(Long userId, EvaluationRequest request) {
        // 临时注释掉订单验证（用于测试）
        // OrdersEntity order = ordersMapper.selectById(request.getOrdersId());
        // if (order == null || !order.getUserId().equals(userId.intValue())) {
        //     throw new IllegalArgumentException("订单不存在或无权评价");
        // }

        System.out.println("=== 创建评价 ===");
        System.out.println("userId: " + userId);
        System.out.println("ordersId: " + request.getOrdersId());
        System.out.println("dishesId: " + request.getDishesId());
        System.out.println("ratingValue: " + request.getRatingValue());
        System.out.println("content: " + request.getContent());

        DishesEvaluationsEntity entity = new DishesEvaluationsEntity();
        entity.setDishesId(request.getDishesId().intValue());
        entity.setUserId(userId.intValue());
        entity.setOrdersId(request.getOrdersId().intValue());
        entity.setContent(request.getContent());
        entity.setRatingValue(request.getRatingValue());
        entity.setReplyStatus(0);
        entity.setCreateTime(LocalDateTime.now());
        evaluationsMapper.insert(entity);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            int orderNum = 1;
            for (String url : request.getImageUrls()) {
                ImagesEntity img = new ImagesEntity();
                img.setDishesEvaluationsId(entity.getId());
                img.setPictureUrl(url);
                img.setNumber(orderNum++);
                imagesMapper.insert(img);
            }
        }

        System.out.println("评价创建成功！ID: " + entity.getId());
    }

    @Transactional
    public void replyByMerchant(Long evaluationId, String replyContent) {
        DishesEvaluationsEntity entity = evaluationsMapper.selectById(evaluationId);
        if (entity == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        entity.setReplyContent(replyContent);
        entity.setReplyTime(LocalDateTime.now());
        entity.setReplyStatus(1);
        evaluationsMapper.updateById(entity);
    }

    private void attachImages(List<EvaluationResponse> evaluations) {
        for (EvaluationResponse ev : evaluations) {
            List<String> urls = imagesMapper.selectList(
                    new LambdaQueryWrapper<ImagesEntity>()
                            .eq(ImagesEntity::getDishesEvaluationsId, ev.getId().intValue())
                            .orderByAsc(ImagesEntity::getNumber))
                    .stream()
                    .map(ImagesEntity::getPictureUrl)
                    .collect(Collectors.toList());
            ev.setImageUrls(urls);
        }
    }
}
