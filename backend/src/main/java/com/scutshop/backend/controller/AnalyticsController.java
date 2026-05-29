package com.scutshop.backend.controller;

import com.scutshop.backend.mapper.OrderMapper;
import com.scutshop.backend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final OrderMapper orderMapper;

    public AnalyticsController(AnalyticsService analyticsService, OrderMapper orderMapper) {
        this.analyticsService = analyticsService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/sales-stats")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    public ResponseEntity<?> salesStats(@RequestParam(value = "range", defaultValue = "30") int days) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("topProducts", orderMapper.selectTopProductsFiltered(10, null, null, null));
        stats.put("dailySales", orderMapper.selectDailySalesRange(days));
        stats.put("weeklySales", orderMapper.selectWeeklySales(12));
        stats.put("monthlySales", orderMapper.selectMonthlySales(12));
        stats.put("dailyOrderCount", orderMapper.selectDailyOrderCount(days));
        stats.put("totalSales", orderMapper.selectTotalSales());
        stats.put("totalOrders", orderMapper.countAll());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/forecast")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    public ResponseEntity<?> forecast(@RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "forecast", defaultValue = "7") int forecastDays) {
        return ResponseEntity.ok(analyticsService.salesForecast(days, forecastDays));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    public ResponseEntity<?> anomalies(@RequestParam(value = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.detectAnomalies(days));
    }

    @GetMapping("/user-profile/{userId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES')")
    public ResponseEntity<?> userProfile(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(analyticsService.userProfile(userId));
    }
}
