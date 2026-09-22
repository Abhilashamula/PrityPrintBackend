package com.pingprint.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PricingController {
    private final JdbcTemplate db;
    public PricingController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/api/pricing")
    public Map<String, Object> pricing() {
        return db.queryForMap("SELECT price_bw_minor, price_color_minor, max_file_mb FROM master_pricing WHERE id = 1");
    }
}