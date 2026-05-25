package com.nomnom.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nomnom.backend.entity.DishesEntity;
import com.nomnom.backend.entity.DishesEvaluationsEntity;
import com.nomnom.backend.entity.DishesPackageEntity;
import com.nomnom.backend.entity.ImagesEntity;
import com.nomnom.backend.entity.OrdersEntity;
import com.nomnom.backend.entity.OrdersItemEntity;
import com.nomnom.backend.entity.UserEntity;
import com.nomnom.backend.mapper.DishesMapper;
import com.nomnom.backend.mapper.DishesEvaluationsMapper;
import com.nomnom.backend.mapper.DishesPackageMapper;
import com.nomnom.backend.mapper.EvaluationReadMapper;
import com.nomnom.backend.mapper.ImagesMapper;
import com.nomnom.backend.mapper.OrdersItemMapper;
import com.nomnom.backend.mapper.OrdersMapper;
import com.nomnom.backend.mapper.UserMapper;
import com.nomnom.backend.model.EvaluationRequest;
import com.nomnom.backend.model.EvaluationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishesEvaluationsService {

    private static final int ROLE_MERCHANT = 2;
    private static final int MESSAGE_TYPE_REVIEW = 4;

    private final DishesEvaluationsMapper evaluationsMapper;
    private final EvaluationReadMapper evaluationReadMapper;
    private final ImagesMapper imagesMapper;
    private final OrdersMapper ordersMapper;
    private final OrdersItemMapper ordersItemMapper;
    private final DishesPackageMapper packageMapper;
    private final DishesMapper dishesMapper;
    private final UserMapper userMapper;
    private final MessagesService messagesService;

    public DishesEvaluationsService(DishesEvaluationsMapper evaluationsMapper,
                                    EvaluationReadMapper evaluationReadMapper,
                                    ImagesMapper imagesMapper,
                                    OrdersMapper ordersMapper,
                                    OrdersItemMapper ordersItemMapper,
                                    DishesPackageMapper packageMapper,
                                    DishesMapper dishesMapper,
                                    UserMapper userMapper,
                                    MessagesService messagesService) {
        this.evaluationsMapper = evaluationsMapper;
        this.evaluationReadMapper = evaluationReadMapper;
        this.imagesMapper = imagesMapper;
        this.ordersMapper = ordersMapper;
        this.ordersItemMapper = ordersItemMapper;
        this.packageMapper = packageMapper;
        this.dishesMapper = dishesMapper;
        this.userMapper = userMapper;
        this.messagesService = messagesService;
    }

    public List<EvaluationResponse> getAllEvaluations() {
        List<EvaluationResponse> list = evaluationReadMapper.selectAllEvaluations();
        attachImages(list);
        return list;
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
        if (request.getOrdersId() == null || request.getDishesId() == null) {
            throw new IllegalArgumentException("订单和菜品不能为空。");
        }
        if (request.getRatingValue() == null || request.getRatingValue() < 1 || request.getRatingValue() > 5) {
            throw new IllegalArgumentException("评分必须在 1 到 5 星之间。");
        }

        OrdersEntity order = ordersMapper.selectById(request.getOrdersId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("订单不存在或无权评价。");
        }
        if (!isPaidOrderStatus(order.getStatus())) {
            throw new IllegalArgumentException("支付完成后即可评价，请先完成订单支付。");
        }
        if (!orderContainsDish(request.getOrdersId(), request.getDishesId())) {
            throw new IllegalArgumentException("只能评价该订单中购买过的菜品。");
        }
        Long existing = evaluationsMapper.selectCount(new LambdaQueryWrapper<DishesEvaluationsEntity>()
                .eq(DishesEvaluationsEntity::getUserId, userId.intValue())
                .eq(DishesEvaluationsEntity::getOrdersId, request.getOrdersId().intValue())
                .eq(DishesEvaluationsEntity::getDishesId, request.getDishesId().intValue()));
        if (existing > 0) {
            throw new IllegalArgumentException("该菜品已经评价过。");
        }

        DishesEvaluationsEntity entity = new DishesEvaluationsEntity();
        entity.setDishesId(request.getDishesId().intValue());
        entity.setUserId(userId.intValue());
        entity.setOrdersId(request.getOrdersId().intValue());
        entity.setContent(request.getContent() != null ? request.getContent().trim() : "");
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

        String dishName = resolveDishName(request.getDishesId());
        String customerName = resolveCustomerName(userId);
        messagesService.createMessageForRole(ROLE_MERCHANT,
                customerName + "评价了「" + dishName + "」：" + request.getRatingValue() + "星，内容：" + entity.getContent(),
                MESSAGE_TYPE_REVIEW);
    }

    @Transactional
    public void replyByMerchant(Long evaluationId, String replyContent) {
        DishesEvaluationsEntity entity = evaluationsMapper.selectById(evaluationId);
        if (entity == null) {
            throw new IllegalArgumentException("评价不存在。");
        }
        entity.setReplyContent(replyContent);
        entity.setReplyTime(LocalDateTime.now());
        entity.setReplyStatus(1);
        evaluationsMapper.updateById(entity);
        messagesService.createMessage(entity.getUserId().longValue(),
                "商家回复了你对「" + resolveDishName(entity.getDishesId().longValue()) + "」的评价：" + replyContent,
                MESSAGE_TYPE_REVIEW);
    }

    @Transactional
    public void deleteEvaluation(Long evaluationId) {
        DishesEvaluationsEntity entity = evaluationsMapper.selectById(evaluationId);
        if (entity == null) {
            throw new IllegalArgumentException("评价不存在。");
        }
        String dishName = resolveDishName(entity.getDishesId().longValue());
        imagesMapper.delete(new LambdaQueryWrapper<ImagesEntity>()
                .eq(ImagesEntity::getDishesEvaluationsId, entity.getId()));
        evaluationsMapper.deleteById(evaluationId);
        messagesService.createMessage(entity.getUserId().longValue(),
                "你对「" + dishName + "」的评价已被商家删除。",
                MESSAGE_TYPE_REVIEW);
    }

    private boolean orderContainsDish(Long orderId, Long dishesId) {
        List<OrdersItemEntity> items = ordersItemMapper.selectList(new LambdaQueryWrapper<OrdersItemEntity>()
                .eq(OrdersItemEntity::getOrdersId, orderId));
        for (OrdersItemEntity item : items) {
            if (item.getDishesPackageId() == null) {
                continue;
            }
            DishesPackageEntity pkg = packageMapper.selectById(item.getDishesPackageId());
            if (pkg != null && pkg.getDishesId() != null && pkg.getDishesId().equals(dishesId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPaidOrderStatus(Integer status) {
        return status != null && (status == 2 || status == 3 || status == 4);
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

    private String resolveDishName(Long dishesId) {
        if (dishesId == null) {
            return "未知菜品";
        }
        DishesEntity dish = dishesMapper.selectById(dishesId.intValue());
        return dish != null && dish.getName() != null ? dish.getName() : "未知菜品";
    }

    private String resolveCustomerName(Long userId) {
        if (userId == null) {
            return "顾客";
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return "顾客";
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername().trim();
        }
        if (user.getAccount() != null && !user.getAccount().trim().isEmpty()) {
            return user.getAccount().trim();
        }
        return "顾客";
    }
}
