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
     * Holt-Winters 加法季节性 + 线性回归混合预测
     * 加法模型对稀疏数据更稳定，不会因近零的季节因子而放大异常
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

        // 计算均值和标准差，检测数据质量
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 999; // 变异系数

        // 数据太稀疏或波动过大，用简单线性回归更可靠
        if (n < 14 || cv > 3.0) {
            return linearRegressionForecast(values, forecastDays);
        }

        // Holt-Winters 加法季节性
        int seasonLength = 7;
        double alpha = 0.2;  // 水平
        double beta = 0.1;   // 趋势
        double gamma = 0.15; // 季节

        // 初始化水平
        double initLevel = 0;
        for (int i = 0; i < seasonLength; i++) initLevel += values.get(i);
        initLevel /= seasonLength;

        // 初始化趋势
        double initTrend = (values.get(seasonLength) - values.get(0)) / seasonLength;

        // 初始化加法季节分量: S_i = Y_i - L
        double[] seasonals = new double[seasonLength];
        for (int i = 0; i < seasonLength; i++) {
            seasonals[i] = values.get(i) - initLevel;
        }

        double level = initLevel;
        double trend = initTrend;

        // 平滑
        for (int i = 0; i < n; i++) {
            int sIdx = i % seasonLength;
            double prevLevel = level;
            level = alpha * (values.get(i) - seasonals[sIdx]) + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
            seasonals[sIdx] = gamma * (values.get(i) - level) + (1 - gamma) * seasonals[sIdx];
        }

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("day", i);
            int sIdx = (n + i - 1) % seasonLength;
            double val = level + i * trend + seasonals[sIdx];
            point.put("amount", Math.max(0, Math.round(val * 100.0) / 100.0));
            forecast.add(point);
        }
        return forecast;
    }

    /** 简单线性回归预测：对稀疏/噪声数据更稳健 */
    private List<Map<String, Object>> linearRegressionForecast(List<Double> values, int forecastDays) {
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("day", i);
            double val = intercept + slope * (n + i);
            point.put("amount", Math.max(0, Math.round(val * 100.0) / 100.0));
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
