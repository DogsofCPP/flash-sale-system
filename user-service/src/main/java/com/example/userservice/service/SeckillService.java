package com.example.userservice.service;

import com.example.userservice.domain.*;
import com.example.userservice.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    private SeckillActivityMapper activityMapper;

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private LuaStockService luaStockService;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    public void initSeckillStock(Long activityId, Long productId) {
        SeckillProduct sp = seckillProductMapper.findByActivityAndProduct(activityId, productId);
        if (sp != null) {
            luaStockService.initStock(activityId, productId, sp.getStock());
            log.info("初始化秒杀库存: activityId={}, productId={}, stock={}", activityId, productId, sp.getStock());
        }
    }

    public void initAllSeckillStock() {
        List<SeckillActivity> activities = activityMapper.findActiveActivities();
        for (SeckillActivity activity : activities) {
            List<SeckillProduct> products = seckillProductMapper.findByActivityId(activity.getId());
            for (SeckillProduct product : products) {
                initSeckillStock(activity.getId(), product.getProductId());
            }
        }
    }

    public SeckillActivity getActivity(Long id) {
        return activityMapper.findById(id);
    }

    public List<SeckillActivity> getAllActivities() {
        return activityMapper.findAll();
    }

    public List<SeckillActivity> getActiveActivities() {
        return activityMapper.findActiveActivities();
    }

    public List<SeckillProduct> getActivityProducts(Long activityId) {
        return seckillProductMapper.findByActivityId(activityId);
    }

    public String seckill(Long userId, Long activityId, Long productId) {
        log.info("用户 {} 参与秒杀: activityId={}, productId={}", userId, activityId, productId);

        SeckillActivity activity = activityMapper.findById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new RuntimeException("活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new RuntimeException("活动已结束");
        }

        SeckillProduct seckillProduct = seckillProductMapper.findByActivityAndProduct(activityId, productId);
        if (seckillProduct == null) {
            throw new RuntimeException("秒杀商品不存在");
        }

        if (!luaStockService.tryCreateOrder(activityId, productId, userId)) {
            throw new RuntimeException("您已下单，请勿重复提交");
        }

        if (luaStockService.isUserLimitExceeded(activityId, productId, userId, seckillProduct.getLimitPerUser())) {
            luaStockService.deleteOrderKey(activityId, productId, userId);
            throw new RuntimeException("您已超过购买限制");
        }

        long result = luaStockService.decreaseStock(activityId, productId, 1);
        if (result < 0) {
            luaStockService.deleteOrderKey(activityId, productId, userId);
            if (result == -1) {
                throw new RuntimeException("商品已抢光");
            } else {
                throw new RuntimeException("库存未初始化，请联系管理员");
            }
        }

        SeckillOrder seckillOrder = new SeckillOrder(userId, activityId, productId, null);
        seckillOrderMapper.insert(seckillOrder);

        String orderNo = generateOrderNo();
        try {
            Product product = productMapper.findById(productId);
            BigDecimal totalAmount = seckillProduct.getSeckillPrice().multiply(BigDecimal.valueOf(1));

            OrderMessage message = new OrderMessage(
                    userId, productId, activityId,
                    product.getName(),
                    seckillProduct.getSeckillPrice(),
                    1, totalAmount, orderNo
            );

            orderMessageProducer.sendOrderMessage(message);

            seckillOrderMapper.updateStatusToSuccess(userId, activityId, productId, orderNo);
            luaStockService.addUserPurchaseCount(activityId, productId, userId);

            log.info("秒杀成功: userId={}, orderNo={}", userId, orderNo);
            return orderNo;
        } catch (Exception e) {
            log.error("创建订单失败，回滚库存: userId={}, productId={}", userId, productId, e);
            luaStockService.increaseStock(activityId, productId, 1);
            seckillOrderMapper.updateStatus(seckillOrder.getId(), 2);
            luaStockService.deleteOrderKey(activityId, productId, userId);
            throw new RuntimeException("订单创建失败: " + e.getMessage());
        }
    }

    public void rollbackStock(Long activityId, Long productId) {
        luaStockService.increaseStock(activityId, productId, 1);
        log.info("库存已回滚: activityId={}, productId={}", activityId, productId);
    }

    private String generateOrderNo() {
        return "SK" + System.currentTimeMillis() + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
