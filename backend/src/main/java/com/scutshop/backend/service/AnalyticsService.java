package com.scutshop.backend.service;

import com.scutshop.backend.mapper.OrderMapper;
import com.scutshop.backend.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AnalyticsService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    public AnalyticsService(OrderMapper orderMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
    }

    /**
     * 用户画像：地域分布、购买力、偏好分类
     */
    public Map<String, Object> userProfile(Long userId) {
        Map<String, Object> profile = new HashMap<>();
        // 购买力：从order表计算
        var orders = orderMapper.selectByUserId(userId, 1000, 0);
        if (orders.isEmpty()) {
            profile.put("level", "new");
            profile.put("message", "暂无购买记录");
            return profile;
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = orders.size();
        for (var o : orders) {
            if (o.getPaymentStatus() == 1 && o.getTotalAmount() != null)
                total = total.add(o.getTotalAmount());
        }
        profile.put("orderCount", count);
        profile.put("totalSpent", total);
        // 分档
        String level = "low";
        if (total.compareTo(new BigDecimal("500")) > 0) level = "medium";
        if (total.compareTo(new BigDecimal("2000")) > 0) level = "high";
        profile.put("level", level);
        return profile;
    }

    /**
     * Holt-Winters 三次指数平滑（乘法季节性），捕捉水平+趋势+周期
     */
    public List<Map<String, Object>> salesForecast(int days, int forecastDays) {
        var dailySales = orderMapper.selectDailySalesRange(days);
        if (dailySales.isEmpty()) return Collections.emptyList();

        List<Double> values = new ArrayList<>();
        for (var row : dailySales) {
            Object amount = row.get("amount");
            if (amount != null) values.add(((Number) amount).doubleValue());
        }
        if (values.isEmpty()) return Collections.emptyList();

        int n = values.size();
        int seasonLength = 7; // 周周期
        if (n < seasonLength * 2) {
            // 数据不足两个周期，退化为 Holt 双指数平滑
            return holtForecast(values, forecastDays);
        }

        // Holt-Winters 乘法季节性
        double alpha = 0.3;  // 水平
        double beta = 0.15;  // 趋势
        double gamma = 0.3;  // 季节

        // 初始化水平：第一个周期的均值
        double initLevel = 0;
        for (int i = 0; i < seasonLength; i++) initLevel += values.get(i);
        initLevel /= seasonLength;

        // 初始化趋势：两个周期之间的平均趋势
        double initTrend = 0;
        for (int i = 0; i < seasonLength; i++) {
            initTrend += (values.get(i + seasonLength) - values.get(i)) / seasonLength;
        }
        initTrend /= seasonLength;

        // 初始化季节因子
        double[] seasonals = new double[seasonLength];
        for (int i = 0; i < seasonLength; i++) {
            seasonals[i] = values.get(i) / initLevel;
        }

        double level = initLevel;
        double trend = initTrend;

        // 对全部历史数据做平滑
        for (int i = 0; i < n; i++) {
            int sIdx = i % seasonLength;
            double prevLevel = level;
            if (seasonals[sIdx] < 0.01) seasonals[sIdx] = 0.01;
            level = alpha * (values.get(i) / seasonals[sIdx]) + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
            seasonals[sIdx] = gamma * (values.get(i) / level) + (1 - gamma) * seasonals[sIdx];
            if (seasonals[sIdx] < 0.01) seasonals[sIdx] = 0.01;
        }

        // 预测
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("day", i);
            int sIdx = (n + i - 1) % seasonLength;
            double val = (level + i * trend) * seasonals[sIdx];
            point.put("amount", Math.max(0, Math.round(val * 100.0) / 100.0));
            forecast.add(point);
        }
        return forecast;
    }

    private List<Map<String, Object>> holtForecast(List<Double> values, int forecastDays) {
        double alpha = 0.4, beta = 0.3;
        double level = values.get(0);
        double trend = values.get(1) - values.get(0);
        for (double v : values) {
            double prevLevel = level;
            level = alpha * v + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("day", i);
            point.put("amount", Math.round((level + i * trend) * 100.0) / 100.0);
            forecast.add(point);
        }
        return forecast;
    }

    /**
     * 检测销售异常（低于均值-2*标准差）
     */
    public List<Map<String, Object>> detectAnomalies(int days) {
        var dailySales = orderMapper.selectDailySalesRange(days);
        if (dailySales.isEmpty()) return Collections.emptyList();

        List<Double> values = new ArrayList<>();
        for (var row : dailySales) {
            Object amount = row.get("amount");
            if (amount != null) values.add(((Number) amount).doubleValue());
        }
        if (values.size() < 3) return Collections.emptyList();

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double threshold = mean - 2 * stdDev;

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) < threshold) {
                Map<String, Object> item = new HashMap<>();
                var row = dailySales.get(i);
                item.put("date", row.get("date"));
                item.put("amount", values.get(i));
                item.put("expected", Math.round(mean * 100.0) / 100.0);
                item.put("threshold", Math.round(threshold * 100.0) / 100.0);
                anomalies.add(item);
            }
        }
        return anomalies;
    }
}
