package com.tecsup.autosmart.controller;

import com.tecsup.autosmart.dto.MetricasResponse;
import com.tecsup.autosmart.service.MetricasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin/metrics")
public class AdminMetricasController {

    private final MetricasService metricasService;

    public AdminMetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping
    public ResponseEntity<MetricasResponse> metricas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(metricasService.delMes(mes));
    }
}