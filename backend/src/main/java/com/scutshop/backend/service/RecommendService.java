package com.scutshop.backend.service;

import com.scutshop.backend.mapper.OrderMapper;
import com.scutshop.backend.mapper.ProductMapper;
import com.scutshop.backend.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    public RecommendService(OrderMapper orderMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
    }

    /**
     * "购买此商品的用户也买了..." 基于order_item共现分析
     */
    public List<Map<String, Object>> frequentlyBoughtTogether(Long productId, int limit) {
        List<Long> orderIds = orderMapper.selectOrderIdsByProductId(productId);
        if (orderIds.isEmpty()) return Collections.emptyList();

        int totalOrders = orderIds.size();

        Map<Long, Long> freqMap = new HashMap<>();
        for (Long oid : orderIds) {
            var items = orderMapper.selectItemsByOrderId(oid);
            for (var item : items) {
                if (!item.getProductId().equals(productId)) {
                    freqMap.merge(item.getProductId(), 1L, Long::sum);
                }
            }
        }

        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Product p = productMapper.selectById(e.getKey());
                    if (p == null || p.getStatus() != 1) return null;
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("name", p.getName());
                    item.put("price", p.getPrice());
                    item.put("imageUrl", p.getImageUrl());
                    item.put("coCount", e.getValue());
                    item.put("coPercent", Math.round(e.getValue() * 10000.0 / totalOrders) / 100.0);
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 基于用户的协同过滤 (Jaccard相似度)
     */
    public List<Product> userBasedCF(Long targetUserId, int limit) {
        // Get all users who made purchases
        var allOrders = orderMapper.selectAll(10000, 0);
        if (allOrders.isEmpty()) return Collections.emptyList();

        // Group: userId -> set of purchased productIds
        Map<Long, Set<Long>> userProducts = new HashMap<>();
        for (var order : allOrders) {
            if (order.getPaymentStatus() != 1) continue;
            var items = orderMapper.selectItemsByOrderId(order.getId());
            for (var item : items) {
                userProducts.computeIfAbsent(order.getUserId(), k -> new HashSet<>())
                        .add(item.getProductId());
            }
        }

        Set<Long> targetProducts = userProducts.getOrDefault(targetUserId, new HashSet<>());
        if (targetProducts.isEmpty()) return Collections.emptyList();

        // Compute Jaccard similarity with other users
        Map<Long, Double> similarities = new HashMap<>();
        for (var entry : userProducts.entrySet()) {
            if (entry.getKey().equals(targetUserId)) continue;
            Set<Long> otherProducts = entry.getValue();
            // Jaccard = |A ∩ B| / |A ∪ B|
            Set<Long> union = new HashSet<>(targetProducts);
            union.addAll(otherProducts);
            Set<Long> intersection = new HashSet<>(targetProducts);
            intersection.retainAll(otherProducts);
            double jaccard = (double) intersection.size() / union.size();
            similarities.put(entry.getKey(), jaccard);
        }

        // Get top N similar users
        List<Long> similarUsers = similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Collect products from similar users not bought by target
        Map<Long, Double> scoreMap = new HashMap<>();
        for (Long simUser : similarUsers) {
            Set<Long> simProducts = userProducts.get(simUser);
            for (Long pid : simProducts) {
                if (!targetProducts.contains(pid)) {
                    scoreMap.merge(pid, similarities.get(simUser), Double::sum);
                }
            }
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> productMapper.selectById(e.getKey()))
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() == 1)
                .collect(Collectors.toList());
    }
}
